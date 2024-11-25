package aws.sdk.kotlin.runtime.config.checksums

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.requestChecksumCalculation
import aws.smithy.kotlin.runtime.client.config.ChecksumConfigOption
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import java.util.*

/**
 * todo
 */
@InternalSdkApi
public suspend fun resolveRequestChecksumCalculation(platform: PlatformProvider = PlatformProvider.System, profile: LazyAsyncValue<AwsProfile>): ChecksumConfigOption {
    val unparsedString = AwsSdkSetting.AwsRequestChecksumCalculation.resolve(platform) ?: profile.get().requestChecksumCalculation
    return unparsedString?.let {
        when (unparsedString.uppercase()) {
            "WHEN_SUPPORTED" -> ChecksumConfigOption.WHEN_SUPPORTED
            "WHEN_REQUIRED" -> ChecksumConfigOption.WHEN_REQUIRED
            else -> throw ConfigurationException(
                "'$it' is not a valid value for request checksum calculation. Valid values are: ${ChecksumConfigOption.entries.toTypedArray()}",
            )
        }
    } ?: ChecksumConfigOption.WHEN_SUPPORTED
}
