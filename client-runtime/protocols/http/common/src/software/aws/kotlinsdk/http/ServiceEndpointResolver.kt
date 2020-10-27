package software.aws.kotlinsdk.http

import software.aws.clientrt.http.Feature
import software.aws.clientrt.http.FeatureKey
import software.aws.clientrt.http.HttpClientFeatureFactory
import software.aws.clientrt.http.SdkHttpClient
import software.aws.clientrt.http.request.HttpRequestPipeline
import software.aws.kotlinsdk.AwsPartition
import software.aws.kotlinsdk.AwsRegion

/**
 *  Http feature for resolving the service endpoint.
 *
 *  TODO: Consider how this type would work with non HTTP protocols.
 */
class ServiceEndpointResolver(private val awsRegionId: String?, private val awsRegionResolver: AwsRegionResolver = DemoAwsRegionResolver(), private val serviceEndpointResolver: AwsRegionEndpointResolver = DemoAwsRegionEndpointResolver()) : Feature {

    class Config {
        internal var awsRegionId: String? = null
    }

    companion object Feature : HttpClientFeatureFactory<Config, ServiceEndpointResolver> {
        override val key: FeatureKey<ServiceEndpointResolver> = FeatureKey("ServiceEndpointResolver")

        override fun create(block: Config.() -> Unit): ServiceEndpointResolver {
            val config = Config().apply(block)
            return ServiceEndpointResolver(config.awsRegionId)
        }
    }

    override fun install(client: SdkHttpClient) {
        client.requestPipeline.intercept(HttpRequestPipeline.Initialize) {
            val regionId = awsRegionId ?: determineDefaultEndpointRegionId()

            context.url.host = serviceEndpointResolver.resolve(
                awsRegionResolver.resolveRegion(regionId) ?: error("Unable to resolve region id")) ?: error("Unable to find endpoint mapping for service")
        }
    }

    // Function to supply a region id if one isn't provided in client configuration.
    // TODO: Implement.  Likely need something per service.
    private fun determineDefaultEndpointRegionId(): String = "us-east-1"
}

/*************************************** Missing ****************************/

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