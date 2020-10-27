package software.aws.kotlinsdk.http

import software.aws.clientrt.http.Feature
import software.aws.clientrt.http.FeatureKey
import software.aws.clientrt.http.HttpClientFeatureFactory
import software.aws.clientrt.http.SdkHttpClient
import software.aws.clientrt.http.request.HttpRequestPipeline
import software.aws.kotlinsdk.regions.AwsRegionEndpointResolver
import software.aws.kotlinsdk.regions.AwsRegionResolver
import software.aws.kotlinsdk.regions.DemoAwsRegionEndpointResolver
import software.aws.kotlinsdk.regions.DemoAwsRegionResolver

/**
 *  Http feature for resolving the service endpoint.
 *
 *  TODO: Determine how/if this type would work with non HTTP protocols.
 */
class ServiceEndpointResolver(
    private val awsRegionId: String?,
    private val awsRegionResolver: AwsRegionResolver = DemoAwsRegionResolver(),
    private val serviceEndpointResolver: AwsRegionEndpointResolver = DemoAwsRegionEndpointResolver()
) : Feature {

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
                awsRegionResolver.resolveRegion(regionId) ?: error("Unable to resolve region id")
            ) ?: error("Unable to find endpoint mapping for service")
        }
    }

    // Function to supply a region id if one isn't provided in client configuration.
    // TODO: Implement.  Likely need something per service.
    private fun determineDefaultEndpointRegionId(): String = "us-east-1"
}

/*************************************** Missing ****************************/

