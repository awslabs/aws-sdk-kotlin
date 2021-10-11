package aws.sdk.kotlin.runtime.http.retries

import aws.sdk.kotlin.runtime.AwsErrorMetadata
import aws.sdk.kotlin.runtime.AwsServiceException
import aws.smithy.kotlin.runtime.ServiceErrorMetadata
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.retries.RetryDirective
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsDefaultRetryPolicyTest {
    @Test
    fun testErrorsByErrorCode() {
        AwsDefaultRetryPolicy.knownErrorTypes.forEach { (errorCode, errorType) ->
            val ex = AwsServiceException()
            ex.sdkErrorMetadata.attributes[AwsErrorMetadata.ErrorCode] = errorCode
            val result = AwsDefaultRetryPolicy.evaluate(Result.failure(ex))
            assertEquals(RetryDirective.RetryError(errorType), result)
        }
    }

    @Test
    fun testErrorsByStatusCode() {
        AwsDefaultRetryPolicy.knownStatusCodes.forEach { (statusCode, errorType) ->
            val modeledStatusCode = HttpStatusCode.fromValue(statusCode)
            val response = HttpResponse(modeledStatusCode, Headers.Empty, HttpBody.Empty)
            val ex = AwsServiceException()
            ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ProtocolResponse] = response
            val result = AwsDefaultRetryPolicy.evaluate(Result.failure(ex))
            assertEquals(RetryDirective.RetryError(errorType), result)
        }
    }
}
