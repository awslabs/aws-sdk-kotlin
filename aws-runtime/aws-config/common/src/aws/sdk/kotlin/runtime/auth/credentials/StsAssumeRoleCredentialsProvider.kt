/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.StsClient
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.model.RegionDisabledException
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.logging.Logger
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.util.Platform
import aws.smithy.kotlin.runtime.util.PlatformEnvironProvider
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

private const val GLOBAL_STS_PARTITION_ENDPOINT = "aws-global"
private const val PROVIDER_NAME = "AssumeRoleProvider"

/**
 * A [CredentialsProvider] that uses another provider to assume a role from the AWS Security Token Service (STS).
 *
 * When asked to provide credentials, this provider will first invoke the inner credentials provider
 * to get AWS credentials for STS. Then, it will call STS to get assumed credentials for the desired role.
 *
 * @param credentialsProvider The underlying provider to use for source credentials
 * @param roleArn The ARN of the target role to assume, e.g. `arn:aws:iam:123456789:role/example`
 * @param region The AWS region to assume the role in. If not set then the global STS endpoint will be used.
 * @param roleSessionName The name to associate with the session. Use the role session name to uniquely identify a session
 * when the same role is assumed by different principals or for different reasons. In cross-account scenarios, the
 * role session name is visible to, and can be logged by the account that owns the role. The role session name is also
 * in the ARN of the assumed role principal.
 * @param externalId A unique identifier that might be required when you assume a role in another account. If the
 * administrator of the account to which the role belongs provided you with an external ID, then provide that value
 * in this parameter.
 * @param duration The expiry duration of the STS credentials. Defaults to 15 minutes if not set.
 * @param httpClientEngine The [HttpClientEngine] to use when making requests to the STS service
 */
@OptIn(ExperimentalTime::class)
public class StsAssumeRoleCredentialsProvider(
    private val credentialsProvider: CredentialsProvider,
    private val roleArn: String,
    private val region: String? = null,
    private val roleSessionName: String? = null,
    private val externalId: String? = null,
    private val duration: Duration = Duration.seconds(DEFAULT_CREDENTIALS_REFRESH_SECONDS),
    private val httpClientEngine: HttpClientEngine? = null
) : CredentialsProvider {

    override suspend fun getCredentials(): Credentials {
        val logger = Logger.getLogger<StsAssumeRoleCredentialsProvider>()
        logger.debug { "retrieving assumed credentials" }

        // NOTE: multi region access points require regional STS endpoints
        val provider = this
        val client = StsClient {
            region = provider.region ?: GLOBAL_STS_PARTITION_ENDPOINT
            credentialsProvider = provider.credentialsProvider
            httpClientEngine = provider.httpClientEngine
        }

        val resp = try {
            client.assumeRole {
                roleArn = provider.roleArn
                externalId = provider.externalId
                roleSessionName = provider.roleSessionName ?: defaultSessionName()
                durationSeconds = provider.duration.inWholeSeconds.toInt()
            }
        } catch (ex: Exception) {
            logger.debug { "sts refused to grant assumed role credentials" }
            when (ex) {
                is RegionDisabledException -> throw ProviderConfigurationException(
                    "STS is not activated in the requested region. Please check your configuration and activate STS in the target region if necessary",
                    ex
                )
                else -> throw CredentialsProviderException("failed to assume role from STS", ex)
            }
        } finally {
            client.close()
        }

        logger.debug { "obtained assumed credentials" }
        val roleCredentials = resp.credentials ?: throw CredentialsProviderException("STS credentials must not be null")

        return Credentials(
            accessKeyId = checkNotNull(roleCredentials.accessKeyId) { "Expected accessKeyId in STS assumeRole response" },
            secretAccessKey = checkNotNull(roleCredentials.secretAccessKey) { "Expected secretAccessKey in STS assumeRole response" },
            sessionToken = roleCredentials.sessionToken,
            expiration = roleCredentials.expiration
        )
    }
}

// role session name must be provided to assume a role, when the user doesn't provide one we choose a name for them
internal fun defaultSessionName(platformEnvironProvider: PlatformEnvironProvider = Platform): String =
    AwsSdkSetting.AwsRoleSessionName.resolve(platformEnvironProvider) ?: "aws-sdk-kotlin-${Instant.now().epochMilliseconds}"
