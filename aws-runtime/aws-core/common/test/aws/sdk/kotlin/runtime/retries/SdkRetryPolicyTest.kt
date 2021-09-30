package aws.sdk.kotlin.runtime.retries

import aws.sdk.kotlin.runtime.AwsErrorMetadata
import aws.sdk.kotlin.runtime.AwsServiceException
import aws.smithy.kotlin.runtime.retries.RetryDirective
import kotlin.test.Test
import kotlin.test.assertEquals

class SdkRetryPolicyTest {
    @Test
    fun testAwsServiceExceptions() {
        SdkRetryPolicy.knownErrorTypes.forEach { (errorCode, errorType) ->
            val ex = AwsServiceException()
            ex.sdkErrorMetadata.attributes[AwsErrorMetadata.ErrorCode] = errorCode
            val result = SdkRetryPolicy.evaluate(Result.failure(ex))
            assertEquals(RetryDirective.RetryError(errorType), result)
        }
    }
}
