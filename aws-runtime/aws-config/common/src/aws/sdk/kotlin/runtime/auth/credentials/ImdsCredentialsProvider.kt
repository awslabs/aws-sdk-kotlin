/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.imds.EC2MetadataError
import aws.sdk.kotlin.runtime.config.imds.ImdsClient
import aws.sdk.kotlin.runtime.config.imds.InstanceMetadataProvider
import aws.sdk.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.logging.Logger
import aws.smithy.kotlin.runtime.serde.json.JsonDeserializer
import aws.smithy.kotlin.runtime.util.Platform
import aws.smithy.kotlin.runtime.util.PlatformEnvironProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

private const val CREDENTIALS_BASE_PATH: String = "/latest/meta-data/iam/security-credentials"
private const val CODE_ASSUME_ROLE_UNAUTHORIZED_ACCESS: String = "AssumeRoleUnauthorizedAccess"

/**
 * [CredentialsProvider] that uses EC2 instance metadata service (IMDS) to provide credentials information.
 * This provider requires that the EC2 instance has an [instance profile](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html#ec2-instance-profile)
 * configured.
 *
 * See [EC2 IAM Roles](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html) for more
 * information.
 *
 * @param profileOverride override the instance profile name. When retrieving credentials, a call must first be made to
 * `<IMDS_BASE_URL>/latest/meta-data/iam/security-credentials`. This returns the instance profile used. If
 * [profileOverride] is set, the initial call to retrieve the profile is skipped and the provided value is used instead.
 * @param client the IMDS client to use to resolve credentials information with
 * @param platformProvider the [PlatformEnvironProvider] instance
 */
public class ImdsCredentialsProvider(
    private val profileOverride: String? = null,
    private val client: Lazy<InstanceMetadataProvider> = lazy { ImdsClient() },
    private val platformProvider: PlatformEnvironProvider = Platform
) : CredentialsProvider, Closeable {
    private val logger = Logger.getLogger<ImdsCredentialsProvider>()

    private val profile = asyncLazy {
        if (profileOverride != null) return@asyncLazy profileOverride
        loadProfile()
    }

    override suspend fun getCredentials(): Credentials {
        if (AwsSdkSetting.AwsEc2MetadataDisabled.resolve(platformProvider) == true) {
            throw CredentialsNotLoadedException("AWS EC2 metadata is explicitly disabled; credentials not loaded")
        }

        val profileName = try {
            profile.get()
        } catch (ex: Exception) {
            throw CredentialsProviderException("failed to load instance profile", ex)
        }

        val payload = client.value.get("$CREDENTIALS_BASE_PATH/$profileName")
        val deserializer = JsonDeserializer(payload.encodeToByteArray())

        return when (val resp = deserializeJsonCredentials(deserializer)) {
            is JsonCredentialsResponse.SessionCredentials -> Credentials(
                resp.accessKeyId,
                resp.secretAccessKey,
                resp.sessionToken,
                resp.expiration
            )
            is JsonCredentialsResponse.Error -> {
                when (resp.code) {
                    CODE_ASSUME_ROLE_UNAUTHORIZED_ACCESS -> throw ProviderConfigurationException("Incorrect IMDS/IAM configuration: [${resp.code}] ${resp.message}. Hint: Does this role have a trust relationship with EC2?")
                    else -> throw CredentialsProviderException("Error retrieving credentials from IMDS: code=${resp.code}; ${resp.message}")
                }
            }
        }
    }

    override fun close() {
        if (client.isInitialized()) {
            client.value.close()
        }
    }

    private suspend fun loadProfile(): String {
        return try {
            client.value.get(CREDENTIALS_BASE_PATH)
        } catch (ex: EC2MetadataError) {
            if (ex.statusCode == HttpStatusCode.NotFound.value) {
                logger.info { "Received 404 from IMDS when loading profile information. Hint: This instance may not have an IAM role associated." }
            }
            throw ex
        }
    }
}
