package software.aws.kotlinsdk.regions

/**
 * The following types are used to facilitate testing of new functionality but will
 * likely exist in other places in final form.
 */

fun interface AwsRegionEndpointResolver {
    fun resolve(region: AwsRegion): String?
}

class DemoAwsRegionEndpointResolver : AwsRegionEndpointResolver {
    override fun resolve(region: AwsRegion): String? {
        //TODO implement
        return "127.0.0.1"
    }
}

/**
 * A facility to return a possible [software.aws.kotlinsdk.AwsRegion] based on an input string.
 */
fun interface AwsRegionResolver {
    fun resolveRegion(id: String): AwsRegion?
}

/**
 * The following function is a sample of what would be custom (not smithy) codegened from endpoints.json.
 */
class DemoAwsRegionResolver : AwsRegionResolver {
    override fun resolveRegion(id: String): AwsRegion? {
        return when (id) {
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
    }
}