package software.aws.kotlinsdk.regions

import software.aws.clientrt.client.ExecutionContext
import software.aws.kotlinsdk.ClientException
import software.aws.kotlinsdk.InternalSdkApi
import software.aws.kotlinsdk.client.AwsAdvancedClientOption
import software.aws.kotlinsdk.client.AwsClientOption

/**
 * This is a provisional Partition type modeled from the Java v2 SDK's PartitionMetadata type.
 * This may be replaced by something else that is generated from the endpoint.json
 * model.
 */
public data class AwsPartition(
    val id: String,
    val name: String,
    val hostName: String,
    val dnsSuffix: String,
    val regionRegex: String // TODO: is there a better type for this than String?
)

/**
 * This is a provisional Region type modeled from the Java v2 SDK's RegionMetadata type.
 * This may be replaced by something else that is generated from the endpoint.json
 * model.
 */
public data class AwsRegion(
    val id: String,
    val domain: String,
    val partition: AwsPartition,
    val description: String
)

/**
 * The following function is a sample of what would be custom (not smithy) codegened from endpoints.json.
 */
internal fun awsRegionByIdResolver(id: String): AwsRegion? =
    when (id) {
        "us-east-1" -> AwsRegion(
            "us-east-1",
            "amazonaws.com",
            AwsPartition(
                "aws",
                "AWS Standard",
                "{service}.{region}.{dnsSuffix}",
                "amazonaws.com",
                "^(us|eu|ap|sa|ca|me|af)\\-\\w+\\-\\d+$"
            ),
            "US East (N. Virginia)"
        )
        else -> null
    }

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
