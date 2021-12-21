/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.profile.LeafProvider
import aws.sdk.kotlin.runtime.auth.credentials.profile.ProfileChain
import aws.sdk.kotlin.runtime.auth.credentials.profile.RoleArn
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.imds.ImdsClient
import aws.sdk.kotlin.runtime.config.profile.loadAwsProfiles
import aws.sdk.kotlin.runtime.config.profile.resolveConfigSource
import aws.sdk.kotlin.runtime.region.resolveRegion
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.util.Platform
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * A [CredentialsProvider] that gets credentials from a profile in `~/.aws/config` or the shared credentials
 * file `~/.aws/credentials`. The locations of these files are configurable via environment or system property on
 * the JVM (see [AwsSdkSetting.AwsConfigFile] and [AwsSdkSetting.AwsSharedCredentialsFile]).
 *
 * This provider is part of the [DefaultChainCredentialsProvider] and usually consumed through that provider. However,
 * it can be instantiated and used standalone as well.
 *
 * NOTE: This provider does not implement any caching. It will reload and reparse the profile from the file system
 * when called. Use [CachedCredentialsProvider] to decorate the profile provider to get caching behavior.
 *
 * This provider supports several credentials formats:
 *
 * ### Credentials defined explicitly within the file
 * ```ini
 * [default]
 * aws_access_key_id = my-access-key
 * aws_secret_access_key = my-secret
 * ```
 *
 * ### Assumed role credentials loaded from a credential source
 * ```ini
 * [default]
 * role_arn = arn:aws:iam:123456789:role/RoleA
 * credential_source = Environment
 * ```
 *
 * ### Assumed role credentials from a source profile
 * ```ini
 * [default]
 * role_arn = arn:aws:iam:123456789:role/RoleA
 * source_profile = base
 *
 * [profile base]
 * aws_access_key_id = my-access-key
 * aws_secret_access_key = my-secret
 * ```
 *
 * Other more complex configurations are possible. See the [Configuration and credential file settings](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)
 * documentation provided by the AWS CLI.
 *
 * @param profileName Override the profile name to use. If not provided it will be resolved internally
 * via environment (see [AwsSdkSetting.AwsProfile]) or defaulted to `default` if not configured.
 * @param region The AWS region to use, this will be resolved internally if not provided.
 * @param platform The platform API provider
 * @param httpClientEngine the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 */
public class ProfileCredentialsProvider(
    private val profileName: String? = null,
    private val region: String? = null,
    private val platform: PlatformProvider = Platform,
    private val httpClientEngine: HttpClientEngine? = null,
) : CredentialsProvider, Closeable {

    private val namedProviders = mapOf(
        "Environment" to EnvironmentCredentialsProvider(platform::getenv),
        "Ec2InstanceMetadata" to ImdsCredentialsProvider(
            profileOverride = profileName,
            client = lazy {
                ImdsClient {
                    platformProvider = platform
                    engine = httpClientEngine
                }
            },
            platformProvider = platform
        ),
        "EcsContainer" to EcsCredentialsProvider(platform, httpClientEngine)
    )

    override suspend fun getCredentials(): Credentials {
        val source = resolveConfigSource(platform, profileName)
        val profiles = loadAwsProfiles(platform, source)
        val chain = ProfileChain.resolve(profiles, source.profile)

        // if profile is overridden for this provider, attempt to resolve it from there first
        val profileOverride = profileName?.let { profiles[it] }
        val region = region ?: profileOverride?.get("region") ?: resolveRegion(platform)

        val leaf = chain.leaf.toCredentialsProvider(region)
        var creds = leaf.getCredentials()

        chain.roles.forEach { roleArn ->
            val assumeProvider = roleArn.toCredentialsProvider(creds, region)
            creds = assumeProvider.getCredentials()
        }

        return creds
    }

    override fun close() {
        namedProviders.forEach { entry ->
            (entry.value as? Closeable)?.close()
        }
    }

    private fun LeafProvider.toCredentialsProvider(region: String): CredentialsProvider = when (this) {
        is LeafProvider.NamedSource -> namedProviders[name] ?: throw ProviderConfigurationException("unknown credentials source: $name")
        is LeafProvider.AccessKey -> StaticCredentialsProvider(credentials)
        is LeafProvider.WebIdentityTokenRole -> StsWebIdentityCredentialsProvider(
            roleArn,
            webIdentityTokenFile,
            region = region,
            roleSessionName = sessionName,
            platformProvider = platform,
            httpClientEngine = httpClientEngine
        )
        is LeafProvider.Sso -> SsoCredentialsProvider(
            accountId = ssoAccountId,
            roleName = ssoRoleName,
            startUrl = ssoStartUrl,
            ssoRegion = ssoRegion,
            httpClientEngine = httpClientEngine,
            platformProvider = platform
        )
    }

    private fun RoleArn.toCredentialsProvider(
        creds: Credentials,
        region: String
    ): CredentialsProvider = StsAssumeRoleCredentialsProvider(
        credentialsProvider = StaticCredentialsProvider(creds),
        roleArn = roleArn,
        region = region,
        roleSessionName = sessionName,
        externalId = externalId,
        httpClientEngine = httpClientEngine
    )
}
