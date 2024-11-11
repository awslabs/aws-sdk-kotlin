package aws.sdk.kotlin.runtime.config.checksums

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.responseChecksumValidation
import aws.smithy.kotlin.runtime.client.config.ChecksumConfigOption
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import java.util.*

/**
 * todo
 */
@InternalSdkApi
public suspend fun resolveResponseChecksumValidation(platform: PlatformProvider = PlatformProvider.System, profile: LazyAsyncValue<AwsProfile>): ChecksumConfigOption? {
    AwsSdkSetting.AwsResponseChecksumValidation.resolve(platform) ?: profile.get().responseChecksumValidation?.let {
        try {
            return ChecksumConfigOption.valueOf(it.uppercase(Locale.getDefault()))
        } catch (_: IllegalArgumentException) {
            throw ConfigurationException(
                "'$it' is not a valid value for response checksum validation. Valid values are: ${ChecksumConfigOption.entries.toTypedArray()}",
            )
        }
    }
    return null
}
