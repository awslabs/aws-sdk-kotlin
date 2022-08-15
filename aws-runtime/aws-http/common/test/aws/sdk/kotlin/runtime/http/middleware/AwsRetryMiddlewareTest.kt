package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.http.retries.AwsDefaultRetryPolicy
import aws.smithy.kotlin.runtime.client.ExecutionContext
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpCall
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.http.sdkHttpClient
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategyOptions
import aws.smithy.kotlin.runtime.retries.delay.DelayProvider
import aws.smithy.kotlin.runtime.retries.delay.StandardRetryTokenBucket
import aws.smithy.kotlin.runtime.retries.delay.StandardRetryTokenBucketOptions
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.tracing.NoOpTraceSpan
import aws.smithy.kotlin.runtime.util.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AwsRetryMiddlewareTest {

    private val mockEngine = object : HttpClientEngineBase("test") {
        override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall {
            val resp = HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.Empty)
            return HttpCall(request, resp, Instant.now(), Instant.now())
        }
    }
    private val client = sdkHttpClient(mockEngine)

    @Test
    fun testItSetsRetryHeaders() = runTest {
        // see retry-header SEP
        val op = SdkHttpOperation.build<Unit, Unit> {
            serializer = UnitSerializer
            deserializer = UnitDeserializer
            context {
                // required operation context
                operationName = "TestOperation"
                service = "TestService"
                traceSpan = NoOpTraceSpan
            }
        }

        val delayProvider = DelayProvider { }
        val strategy = StandardRetryStrategy(
            StandardRetryStrategyOptions.Default,
            StandardRetryTokenBucket(StandardRetryTokenBucketOptions.Default),
            delayProvider,
        )
        val maxAttempts = strategy.options.maxAttempts

        op.install(AwsRetryMiddleware(strategy, AwsDefaultRetryPolicy))

        op.roundTrip(client, Unit)
        val calls = op.context.attributes[HttpOperationContext.HttpCallList]
        val sdkRequestId = op.context.sdkRequestId

        assertTrue(calls.all { it.request.headers[AMZ_SDK_INVOCATION_ID_HEADER] == sdkRequestId })
        calls.forEachIndexed { idx, call ->
            assertEquals("attempt=${idx + 1}; max=$maxAttempts", call.request.headers[AMZ_SDK_REQUEST_HEADER])
        }
    }
}
