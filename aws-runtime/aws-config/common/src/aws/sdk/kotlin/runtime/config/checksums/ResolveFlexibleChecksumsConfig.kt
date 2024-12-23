package aws.sdk.kotlin.runtime.config.checksums

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.requestChecksumCalculation
import aws.sdk.kotlin.runtime.config.profile.responseChecksumValidation
import aws.smithy.kotlin.runtime.client.config.RequestHttpChecksumConfig
import aws.smithy.kotlin.runtime.client.config.ResponseHttpChecksumConfig
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Attempts to resolve requestChecksumCalculation from the specified sources.
 * @return requestChecksumCalculation setting if found, the default value if not.
 */
@InternalSdkApi
public suspend fun resolveRequestChecksumCalculation(
    platform: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile>,
): RequestHttpChecksumConfig {
    val unparsedString = AwsSdkSetting.AwsRequestChecksumCalculation.resolve(platform) ?: profile.get().requestChecksumCalculation
    return parseRequestHttpChecksumConfig(unparsedString)
}

private fun parseRequestHttpChecksumConfig(unparsedString: String?): RequestHttpChecksumConfig =
    when (unparsedString?.uppercase()) {
        null -> RequestHttpChecksumConfig.WHEN_SUPPORTED
        "WHEN_SUPPORTED" -> RequestHttpChecksumConfig.WHEN_SUPPORTED
        "WHEN_REQUIRED" -> RequestHttpChecksumConfig.WHEN_REQUIRED
        else -> throw ConfigurationException(
            "'$unparsedString' is not a valid value for 'requestChecksumCalculation'. Valid values are: ${RequestHttpChecksumConfig.entries}",
        )
    }

/**
 * Attempts to resolve responseChecksumValidation from the specified sources.
 * @return responseChecksumValidation setting if found, the default value if not.
 */
@InternalSdkApi
public suspend fun resolveResponseChecksumValidation(
    platform: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile>,
): ResponseHttpChecksumConfig {
    val unparsedString = AwsSdkSetting.AwsResponseChecksumValidation.resolve(platform) ?: profile.get().responseChecksumValidation
    return parseResponseHttpChecksumConfig(unparsedString)
}

private fun parseResponseHttpChecksumConfig(unparsedString: String?): ResponseHttpChecksumConfig =
    when (unparsedString?.uppercase()) {
        null -> ResponseHttpChecksumConfig.WHEN_SUPPORTED
        "WHEN_SUPPORTED" -> ResponseHttpChecksumConfig.WHEN_SUPPORTED
        "WHEN_REQUIRED" -> ResponseHttpChecksumConfig.WHEN_REQUIRED
        else -> throw ConfigurationException(
            "'$unparsedString' is not a valid value for 'responseChecksumValidation'. Valid values are: ${ResponseHttpChecksumConfig.entries}",
        )
    }
