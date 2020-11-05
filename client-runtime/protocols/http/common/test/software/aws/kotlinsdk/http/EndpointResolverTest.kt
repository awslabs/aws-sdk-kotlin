package software.aws.kotlinsdk.http

import software.aws.clientrt.http.*
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.response.HttpResponse
import software.aws.kotlinsdk.regions.AwsPartition
import software.aws.kotlinsdk.regions.AwsRegion
import software.aws.kotlinsdk.regions.AwsRegionEndpointResolver
import software.aws.kotlinsdk.regions.AwsRegionResolver
import software.aws.kotlinsdk.testing.runSuspendTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

const val TEST_ENDPOINT = "127.0.0.1"

@OptIn(ExperimentalStdlibApi::class)
class EndpointResolverTest {

    class TestAwsRegionEndpointResolver : AwsRegionEndpointResolver {
        override fun resolve(region: AwsRegion): String? {
            return TEST_ENDPOINT
        }
    }

    class TestAwsRegionResolver : AwsRegionResolver {
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

    @Test
    fun `it sets the host to the expected endpoint`(): Unit = runSuspendTest {
        val requestBuilder = HttpRequestBuilder()

        val testResponse = HttpResponse(HttpStatusCode.fromValue(200), Headers {}, HttpBody.Empty, requestBuilder.build())

        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse {
                assertEquals(requestBuilder.url.host, TEST_ENDPOINT)
                return testResponse
            }
        }

        val client = sdkHttpClient(mockEngine) {
            install(ServiceEndpointResolver)
        }

        val response = client.roundTrip<HttpResponse>(requestBuilder)

        assertNotNull(response)
    }
}
