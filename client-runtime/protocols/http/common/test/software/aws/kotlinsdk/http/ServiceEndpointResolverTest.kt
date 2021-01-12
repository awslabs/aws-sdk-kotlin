package software.aws.kotlinsdk.http

import software.aws.clientrt.client.ExecutionContext
import software.aws.clientrt.http.*
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.response.HttpResponse
import software.aws.kotlinsdk.client.AwsClientOption
import software.aws.kotlinsdk.endpoint.Endpoint
import software.aws.kotlinsdk.endpoint.EndpointResolver
import software.aws.kotlinsdk.testing.runSuspendTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalStdlibApi::class)
class ServiceEndpointResolverTest {

    @Test
    fun `it sets the host to the expected endpoint`(): Unit = runSuspendTest {
        val requestBuilder = HttpRequestBuilder()

        val expectedHost = "test.com"
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse {
                assertEquals(expectedHost, requestBuilder.url.host)
                assertEquals(expectedHost, requestBuilder.headers["Host"])
                assertEquals("https", requestBuilder.url.scheme.protocolName)
                return HttpResponse(HttpStatusCode.fromValue(200), Headers {}, HttpBody.Empty, requestBuilder.build())
            }
        }

        val client = sdkHttpClient(mockEngine) {
            install(ServiceEndpointResolver) {
                serviceId = "TestService"
                resolver = object : EndpointResolver {
                    override suspend fun resolve(service: String, region: String): Endpoint {
                        return Endpoint("test.com", "https")
                    }
                }
            }
        }

        val ctx = ExecutionContext()
        ctx[AwsClientOption.Region] = "us-east-1"
        val response = client.roundTrip<HttpResponse>(ctx, requestBuilder)
        assertNotNull(response)
    }
}
