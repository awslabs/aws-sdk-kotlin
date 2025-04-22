package aws.sdk.kotlin.runtime.config.auth

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.authSchemePreference
import aws.smithy.kotlin.runtime.auth.AuthSchemeId
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

@InternalSdkApi
public suspend fun resolveAuthSchemePreference(platform: PlatformProvider = PlatformProvider.System, profile: LazyAsyncValue<AwsProfile>): List<AuthSchemeId> {
    val content = AwsSdkSetting.AwsAuthSchemePreference.resolve(platform) ?: profile.get().authSchemePreference

    return content
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.mapNotNull { AUTH_SCHEME_NAME_MAP[it] }
        ?.toList()
        ?: emptyList()
}

/**
 * Map of lowercase auth scheme _name_ (not namespace) to auth scheme ID
 */
private val AUTH_SCHEME_NAME_MAP = mapOf<String, AuthSchemeId>(
    "sigv4" to AuthSchemeId.AwsSigV4,
    "sigv4a" to AuthSchemeId.AwsSigV4Asymmetric,
    "httpbearerauth" to AuthSchemeId.HttpBearer,
    "noauth" to AuthSchemeId.Anonymous,
    "httpbasicauth" to AuthSchemeId.HttpBasic,
    "httpdigestauth" to AuthSchemeId.HttpDigest,
    "httpapikey" to AuthSchemeId.HttpApiKey,
)
