/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.imds.EC2MetadataError
import aws.sdk.kotlin.runtime.config.imds.ImdsClient
import aws.sdk.kotlin.runtime.config.imds.InstanceMetadataProvider
import aws.sdk.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.logging.Logger
import aws.smithy.kotlin.runtime.serde.json.JsonDeserializer
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.Platform
import aws.smithy.kotlin.runtime.util.PlatformEnvironProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

private const val CREDENTIALS_BASE_PATH: String = "/latest/meta-data/iam/security-credentials"
private const val CODE_ASSUME_ROLE_UNAUTHORIZED_ACCESS: String = "AssumeRoleUnauthorizedAccess"
private const val PROVIDER_NAME = "IMDSv2"
private const val STATIC_STABILITY_LOG_MESSAGE: String = "Attempting credential expiration extension due to a " +
        "credential service availability issue. A refresh of these credentials will be attempted again in %d minutes."

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
 * @param client the IMDS client to use to resolve credentials information with. This provider takes ownership over
 * the lifetime of the given [ImdsClient] and will close it when the provider is closed.
 * @param platformProvider the [PlatformEnvironProvider] instance
 */
public class ImdsCredentialsProvider(
    private val profileOverride: String? = null,
    private val client: Lazy<InstanceMetadataProvider> = lazy { ImdsClient() },
    private val platformProvider: PlatformEnvironProvider = Platform,
    private var previousCredentials: Credentials? = null,
    private val clock: Clock = Clock.System,
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

        // if we have previously served IMDS credentials and it's not time for a refresh, just return the previous credentials
        if (previousCredentials != null
            && previousCredentials!!.nextRefresh != null
            && clock.now() < previousCredentials!!.nextRefresh!!) {
            return previousCredentials as Credentials
        }

        val profileName = try {
            profile.get()
        } catch (ex: Exception) {
            when {
                ex is IOException
                || (ex is EC2MetadataError && ex.statusCode == HttpStatusCode.InternalServerError.value) -> {
                    previousCredentials = previousCredentials?.copy(nextRefresh = clock.now() + DEFAULT_CREDENTIALS_REFRESH_SECONDS.seconds)
                    return previousCredentials ?: throw CredentialsProviderException("failed to load instance profile", ex)
                }
                else -> throw CredentialsProviderException("failed to load instance profile", ex)
            }
        }

        val payload = try {
            client.value.get("$CREDENTIALS_BASE_PATH/$profileName")
        } catch (ex: Exception) {
            when {
                ex is IOException
                || (ex is EC2MetadataError && ex.statusCode == HttpStatusCode.InternalServerError.value) -> {
                    previousCredentials = previousCredentials?.copy(nextRefresh = clock.now() + DEFAULT_CREDENTIALS_REFRESH_SECONDS.seconds)
                    return previousCredentials ?: throw CredentialsProviderException("failed to load credentials", ex)
                }
                else -> throw CredentialsProviderException("failed to load credentials", ex)
            }
        }

        val deserializer = JsonDeserializer(payload.encodeToByteArray())

        return when (val resp = deserializeJsonCredentials(deserializer)) {
            is JsonCredentialsResponse.SessionCredentials -> {
                var creds = Credentials(
                    resp.accessKeyId,
                    resp.secretAccessKey,
                    resp.sessionToken,
                    resp.expiration,
                    PROVIDER_NAME,
                )

                if (creds.expiration!! < clock.now()) {
                    // let the user know we will be using expired credentials
                    logger.info { STATIC_STABILITY_LOG_MESSAGE.format(DEFAULT_CREDENTIALS_REFRESH_SECONDS / 60) }
                    creds = creds.copy(nextRefresh = clock.now() + DEFAULT_CREDENTIALS_REFRESH_SECONDS.seconds)
                }

                previousCredentials = creds
                creds
            }
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
