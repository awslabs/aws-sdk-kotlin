package software.aws.kotlinsdk.http

import software.aws.clientrt.http.*
import software.aws.clientrt.http.request.HttpRequestPipeline
import software.aws.clientrt.util.get
import software.aws.kotlinsdk.client.AwsClientOption
import software.aws.kotlinsdk.endpoint.EndpointResolver

/**
 *  Http feature for resolving the service endpoint.
 */
public class ServiceEndpointResolver(
    config: Config
) : Feature {

    private val serviceId: String = requireNotNull(config.serviceId) { "ServiceId must not be null" }
    private val resolver: EndpointResolver = requireNotNull(config.resolver) { "EndpointResolver must not be null" }

    public class Config {
        /**
         * The AWS service ID to resolve endpoints for
         */
        public var serviceId: String? = null

        /**
         * The resolver to use
         */
        public var resolver: EndpointResolver? = null
    }

    public companion object Feature : HttpClientFeatureFactory<Config, ServiceEndpointResolver> {
        override val key: FeatureKey<ServiceEndpointResolver> = FeatureKey("ServiceEndpointResolver")

        override fun create(block: Config.() -> Unit): ServiceEndpointResolver {
            val config = Config().apply(block)
            return ServiceEndpointResolver(config)
        }
    }

    override fun install(client: SdkHttpClient) {
        client.requestPipeline.intercept(HttpRequestPipeline.Initialize) {
            val region = context.executionContext[AwsClientOption.Region]
            val endpoint = resolver.resolve(serviceId, region)
            subject.url.scheme = Protocol.parse(endpoint.protocol)
            subject.url.host = endpoint.hostname
            subject.headers["Host"] = endpoint.hostname
        }
    }
}
