package software.aws.kotlinsdk.regions

import software.aws.clientrt.client.ExecutionContext
import software.aws.kotlinsdk.ClientException
import software.aws.kotlinsdk.InternalSdkApi
import software.aws.kotlinsdk.client.AwsAdvancedClientOption
import software.aws.kotlinsdk.client.AwsClientOption

/**
 * Attempt to resolve the region to make requests to.
 *
 * Regions are resolved in the following order:
 *   1. From the existing [ctx]
 *   2. From the region [config]
 *   3. Using default region detection (only if-enabled)
 */
@InternalSdkApi
public fun resolveRegionForOperation(ctx: ExecutionContext, config: RegionConfig): String {
    // favor the context if it's already set
    val regionFromCtx = ctx.getOrNull(AwsClientOption.Region)
    if (regionFromCtx != null) return regionFromCtx

    // use the default from the service config if configured
    if (config.region != null) return config.region!!

    // attempt to detect
    val allowDefaultRegionDetect = ctx.getOrNull(AwsAdvancedClientOption.EnableDefaultRegionDetection) ?: true
    if (!allowDefaultRegionDetect) {
        throw ClientException("No region was configured and region detection has been disabled")
    }

    TODO("default region detection has not been implemented yet")
}
