package aws.sdk.kotlin.runtime.http.middleware

import aws.smithy.kotlin.runtime.http.middleware.Retry
import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.http.request.header
import aws.smithy.kotlin.runtime.io.Handler
import aws.smithy.kotlin.runtime.retries.RetryStrategy
import aws.smithy.kotlin.runtime.retries.policy.RetryPolicy
import aws.smithy.kotlin.runtime.util.InternalApi
import aws.smithy.kotlin.runtime.util.get

/**
 * The per/operation unique client side ID header name. This will match
 * the [HttpOperationContext.SdkRequestId]
 */
internal const val AMZ_SDK_INVOCATION_ID_HEADER = "amz-sdk-invocation-id"

/**
 * Details about the current request such as the attempt number, maximum possible attempts, ttl, etc
 */
internal const val AMZ_SDK_REQUEST_HEADER = "amz-sdk-request"

/**
 * Retry requests with the given strategy and policy. This middleware customizes the default [Retry] implementation
 * to add AWS specific retry headers
 *
 * @param strategy the [RetryStrategy] to retry failed requests with
 * @param policy the [RetryPolicy] used to determine when to retry
 */
@InternalApi
public class AwsRetryMiddleware<O>(
    strategy: RetryStrategy,
    policy: RetryPolicy<Any?>,
) : Retry<O>(strategy, policy) {

    override suspend fun <H : Handler<SdkHttpRequest, O>> handle(request: SdkHttpRequest, next: H): O {
        request.subject.header(AMZ_SDK_INVOCATION_ID_HEADER, request.context.sdkRequestId)
        return super.handle(request, next)
    }

    override fun onAttempt(request: SdkHttpRequest, attempt: Int) {
        // setting ttl would never be accurate, just set what we know which is attempt and maybe max attempt
        val maxAttempts = strategy.options.maxAttempts?.let { "; max=$it" } ?: ""
        request.subject.header(AMZ_SDK_REQUEST_HEADER, "attempt=${attempt}$maxAttempts")
    }
}
