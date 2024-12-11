package aws.sdk.kotlin.runtime.config.checksums

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.requestChecksumCalculation
import aws.sdk.kotlin.runtime.config.profile.responseChecksumValidation
import aws.smithy.kotlin.runtime.client.config.HttpChecksumConfigOption
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Attempts to resolve requestChecksumCalculation from the specified sources.
 * @return requestChecksumCalculation setting if found, the default value if not.
 */
@InternalSdkApi
public suspend fun resolveRequestChecksumCalculation(platform: PlatformProvider = PlatformProvider.System, profile: LazyAsyncValue<AwsProfile>): HttpChecksumConfigOption {
    val unparsedString = AwsSdkSetting.AwsRequestChecksumCalculation.resolve(platform) ?: profile.get().requestChecksumCalculation
    return parseHttpChecksumConfigOption(unparsedString, "requestChecksumCalculation")
}

/**
 * Attempts to resolve responseChecksumValidation from the specified sources.
 * @return responseChecksumValidation setting if found, the default value if not.
 */
@InternalSdkApi
public suspend fun resolveResponseChecksumValidation(platform: PlatformProvider = PlatformProvider.System, profile: LazyAsyncValue<AwsProfile>): HttpChecksumConfigOption {
    val unparsedString = AwsSdkSetting.AwsResponseChecksumValidation.resolve(platform) ?: profile.get().responseChecksumValidation
    return parseHttpChecksumConfigOption(unparsedString, "responseChecksumValidation")
}

private fun parseHttpChecksumConfigOption(unparsedString: String?, configOption: String): HttpChecksumConfigOption =
    when (unparsedString?.uppercase()) {
        null -> HttpChecksumConfigOption.WHEN_SUPPORTED
        "WHEN_SUPPORTED" -> HttpChecksumConfigOption.WHEN_SUPPORTED
        "WHEN_REQUIRED" -> HttpChecksumConfigOption.WHEN_REQUIRED
        else -> throw ConfigurationException(
            "'$unparsedString' is not a valid value for $configOption. Valid values are: ${HttpChecksumConfigOption.entries}",
        )
    }
