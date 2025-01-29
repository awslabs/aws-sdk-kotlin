# MD5 checksum interceptor

A recent update to the AWS SDK for Kotlin removed support for MD5 checksums in favor of newer algorithms. This may
affect SDK compatibility with third-party "S3-like" services, particularly when invoking the
[`DeleteObjects`](https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjects.html) operation. If you still
require MD5 checksums for S3-like services, you may re-enable them by writing a
[a custom interceptor](https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/interceptors.html).

## Example interceptor code

The following code defines an interceptor which calculates MD5 checksums for S3's `DeleteObjects` operation:

```kotlin
@OptIn(InternalApi::class)
class DeleteObjectsMd5Interceptor : HttpInterceptor {
    companion object {
        private const val MD5_HEADER = "Content-MD5"
        private const val OTHER_CHECKSUMS_PREFIX = "x-amz-checksum-"
        private const val TRAILER_HEADER = "x-amz-trailer"
    }

    override suspend fun modifyBeforeSigning(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        // Only execute for the `DeleteObjects` operation
        if (context.executionContext.operationName != "DeleteObjects") return context.protocolRequest

        val body = context.protocolRequest.body
        val newRequest = context.protocolRequest.toBuilder()

        // Remove any conflicting headers
        removeOtherChecksums(newRequest.headers)
        removeOtherChecksums(newRequest.trailingHeaders)

        newRequest
            .headers
            .getAll(TRAILER_HEADER)
            .orEmpty()
            .filter { it.startsWith(OTHER_CHECKSUMS_PREFIX, ignoreCase = true) }
            .forEach { newRequest.headers.remove(TRAILER_HEADER, it) }
        newRequest.headers.removeKeysWithNoEntries()

        if (body.isEligibleForAwsChunkedStreaming) {
            // Calculate MD5 while streaming, append as a trailing header

            val parentJob = context.executionContext.coroutineContext.job
            val deferredHash = CompletableDeferred<String>(parentJob)

            newRequest.body = body.toHashingBody(Md5(), body.contentLength).toCompletingBody(deferredHash)
            newRequest.headers.append(TRAILER_HEADER, MD5_HEADER)
            newRequest.trailingHeaders[MD5_HEADER] = deferredHash
        } else {
            val hash = if (body.isOneShot) {
                // One-shot stream must be fully read into memory, hashed, and then body replaced with in-memory bytes

                val bytes = body.readAll() ?: byteArrayOf()
                newRequest.body = bytes.toHttpBody()
                bytes.md5().encodeBase64String()
            } else {
                // All other streams can be converted to a channel for which the hash is computed eagerly

                val scope = context.executionContext
                val channel = requireNotNull(body.toSdkByteReadChannel(scope)) { "Cannot convert $body to channel" }
                channel.rollingHash(Md5()).encodeBase64String()
            }

            newRequest.headers[MD5_HEADER] = hash
        }

        return newRequest.build()
    }

    private fun removeOtherChecksums(source: ValuesMapBuilder<*>) =
        source
            .entries()
            .map { it.key }
            .filter { it.startsWith(OTHER_CHECKSUMS_PREFIX, ignoreCase = true) }
            .forEach { source.remove(it) }
}
```

A few notes about particular parts of this code:

* `@OptIn(InternalApi::class)`

  This example makes use of several SDK APIs which are public but not supported for
  external use. Thus, calling code must [opt in](https://kotlinlang.org/docs/opt-in-requirements.html#opt-in-to-api) to
  successfully build.


* `if (context.executionContext.operationName != "DeleteObjects") return context.protocolRequest`

  MD5 checksums are generally only required for `DeleteObjects` invocations on third-party S3-like services. If you
  require MD5 for more operations, adjust this predicate accordingly.


* `if (body.isOneShot)`

  Some streaming payloads come from "one-shot" sources, meaning they cannot be rewound or replayed. This presents
  particular challenges for calculating checksums and for retrying requests which previously failed (e.g., because of a
  transient condition like connection drops or throttling). The only way to correctly handle such payloads is to read
  them completely into memory and then calculate the checksum. This may cause memory issues for very large payloads or
  resource-constrained environments.

## Using the interceptor

Once the interceptor is written, it may be added to an S3 client by way of client config:

```kotlin
val s3 = S3Client.fromEnvironment {
    interceptors += DeleteObjectsMd5Interceptor()
}

s3.deleteObjects { ... } // Will calculate and send MD5 checksum for request
```
