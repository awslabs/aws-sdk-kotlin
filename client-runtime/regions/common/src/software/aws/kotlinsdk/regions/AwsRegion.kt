package software.aws.kotlinsdk.regions

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
