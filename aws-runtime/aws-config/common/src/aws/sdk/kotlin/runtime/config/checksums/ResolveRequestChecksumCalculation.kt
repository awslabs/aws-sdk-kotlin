package aws.sdk.kotlin.runtime.config.checksums

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.requestChecksumCalculation
import aws.smithy.kotlin.runtime.client.config.RequestChecksumCalculation
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import java.util.*

/**
 * todo
 */
@InternalSdkApi
public suspend fun resolveRequestChecksumCalculation(platform: PlatformProvider = PlatformProvider.System, profile: LazyAsyncValue<AwsProfile>): RequestChecksumCalculation? {
    AwsSdkSetting.AwsRequestChecksumCalculation.resolve(platform) ?: profile.get().requestChecksumCalculation?.let {
        try {
            return RequestChecksumCalculation.valueOf(it.uppercase(Locale.getDefault()))
        } catch (_: IllegalArgumentException) {
            throw ConfigurationException("'$it' is not a valid value for request checksum calculation. Valid values are: 'WHEN_SUPPORTED' & 'WHEN_REQUIRED'")
        }
    }
    return null
}

