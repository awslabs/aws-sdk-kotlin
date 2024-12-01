# SDK-specific Retry Design

* **Type**: Design
* **Author(s)**: Ian Botsford

# Abstract

The AWS SDK for Kotlin uses a specialization of the generalized
[**smithy-kotlin** Retry Design](https://github.com/smithy-lang/smithy-kotlin/blob/main/docs/design/retries.md). This
document covers those specializations (but does not re-hash the generalized design).

# SDK implementation

The SDK uses the following customizations/specializations over the generalized
[**smithy-kotlin** Retry Design](https://github.com/smithy-lang/smithy-kotlin/blob/main/docs/design/retries.md):

## Retry policy

The generalized `StandardRetryPolicy` is subclassed to provide support for information only available in AWS-specific
exception types:

```kotlin
object AwsDefaultRetryPolicy : StandardRetryPolicy() {
  internal val knownErrorTypes = mapOf(
    "BandwidthLimitExceeded" to Throttling,
    "RequestTimeoutException" to Timeout,
    "TooManyRequestsException" to Throttling,
    …
  )

  override fun evaluateOtherExceptions(ex: Throwable): RetryDirective? = when (ex) {
    is AwsServiceException -> evaluateAwsServiceException(ex)
    else -> null
  }

  private fun evaluateAwsServiceException(ex: AwsServiceException): RetryDirective? = with(ex.sdkErrorMetadata) {
    knownErrorTypes[errorCode]?.let { RetryDirective.RetryError(it) }
  }
}
```

This policy utilizes the error code provided in the exception to derive a retry directive based on a known list. This
list may grow/change over time.

# Revision history

* 9/27/2021 - Created
