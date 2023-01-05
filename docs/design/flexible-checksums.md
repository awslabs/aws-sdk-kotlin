# Flexible Checksums Design

* **Type**: Design
* **Author**: Matas Lauzadis

# Abstract

[Flexible checksums](https://aws.amazon.com/blogs/aws/new-additional-checksum-algorithms-for-amazon-s3/) are a feature 
that allows users and services to configure checksum validation for HTTP requests and responses. To use the feature, 
AWS services add an [`httpChecksum` trait](https://smithy.io/2.0/aws/aws-core.html#aws-protocols-httpchecksum-trait) to their Smithy models. 
Users may then opt-in to sending request checksums or validating response checksums.

This document covers the design for supporting flexible checksums in the AWS SDK for Kotlin. 

# `httpChecksum` Trait

AWS services use the `httpChecksum` trait on their Smithy operations to enable flexible checksums.
There are four properties of this trait:
- `requestChecksumRequired` identifies if a checksum is required for the HTTP request
- `requestAlgorithmMember` identifies the member which conveys the checksum algorithm to use when sending checksums
- `requestValidationModeMember` identifies the member which conveys the opt-in status for validating checksums returned in the HTTP response
- `responseAlgorithms` identifies a list of strings of checksum algorithms that are used for response validation

### Deprecating `httpChecksumRequired`

Before flexible checksums, services used the [`httpChecksumRequired` trait](https://smithy.io/2.0/spec/http-bindings.html#httpchecksumrequired-trait) to require a checksum in the request. 
This is computed using the MD5 algorithm and injected in the request under the `Content-MD5` header. 

This `httpChecksumRequired` trait is now deprecated. AWS services should use the `httpChecksum` trait's
`requestChecksumRequired` property instead.

The `requestChecksumRequired` property being set to `true` or the `httpChecksumRequired` trait being present on an operation
means a checksum is required for that operation.

If a checksum is required, and the user does not opt-in to using flexible checksums, the SDK will continue the legacy behavior
of injecting the `Content-MD5` header.
## Checksum Algorithms

The SDK needs to support the following checksum algorithms: CRC32C, CRC32, SHA-1, SHA-256

All of them are [already implemented for JVM](https://github.com/awslabs/smithy-kotlin/tree/5773afb348c779b9e4aa9689836844f21a571908/runtime/hashing/jvm/src/aws/smithy/kotlin/runtime/hashing).

As part of this feature, `CRC32C` was implemented in **smithy-kotlin** [PR#724](https://github.com/awslabs/smithy-kotlin/pull/724). 
This algorithm is essentially the same as `CRC32`, but uses a different polynomial under the hood. 
The SDK uses [`java.util.zip`'s implementation of CRC32](https://docs.oracle.com/javase/8/docs/api/java/util/zip/CRC32.html), 
but this package only began shipping `CRC32C` in Java 9. The SDK wants to support Java 8 at a minimum, so this was implemented
rather than imported as a dependency (which is also [what the Java SDK did](https://github.com/aws/aws-sdk-java-v2/blob/ecc12b43a4aedc433c39742a2ae1361bd8d17991/core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/checksums/factory/SdkCrc32C.java)).

### Header Name

The header name used to set the checksum value is `x-amz-checksum-<checksum_algorithm_name>`. For example, if the checksum was computed 
using `SHA256`, the header containing the checksum will be `x-amz-checksum-sha256`.

# Implementation

This feature can be implemented by adding two new middleware: one for calculating checksums for requests, and one for
validating checksums present in responses.

## Requests

During an HTTP request, the SDK first needs to check if the user has opted-in to sending checksums. If they have not opted-in,
but the operation has the `requestChecksumRequired` property set, the SDK will fall back to the legacy behavior of computing the MD5 checksum.

### Middleware
A new middleware is introduced at the `mutate` stage. This stage of the HTTP request lifecycle represents the last chance
to modify the request before it is sent on the network.

There are many middleware which operate at the `mutate` stage. It is important that this new middleware come before 
[`AwsSigningMiddleware`](https://github.com/awslabs/smithy-kotlin/blob/5773afb348c779b9e4aa9689836844f21a571908/runtime/auth/aws-signing-common/common/src/aws/smithy/kotlin/runtime/auth/awssigning/middleware/AwsSigningMiddleware.kt#L26)
because that middleware is dependent on the header values set in this new middleware (specifically `x-amz-trailer`).

The SDK exposes an `order` integer parameter which is used to model dependencies between middleware.
The `order` of `AwsSigningMiddleware` has already been set to 126, which ensures it will be executed towards the end of the mutate middleware stack,
after this flexible checksums middleware has run.

### Normal vs. Streaming Requests

The request checksum should be sent as either as a header or trailing header. The following table lays out all the possible cases
of where the checksum should be placed.

| Payload Type | Authorization Type | Location of Checksum |
|--------------|--------------------|----------------------|
| Normal       | Header-based       | Header               |
| Normal       | Unsigned           | Header               |
| Streaming    | Header-based       | Header               |
| Streaming    | Streaming-signing  | Trailer              |
| Streaming    | Unsigned           | Trailer              |

#### Normal Requests
For all normal requests, the checksum should be injected into the header.

#### Streaming Requests
For streaming requests which are either streaming-signing or unsigned, the checksum must be sent as a trailing header via `aws-chunked` encoding.

To indicate that a trailing header will be sent, the SDK sets the `x-amz-trailer` header to a string of comma-delimited trailing header names.
The service uses this header to parse the trailing headers that are sent later.

For flexible checksums, checksum header name will be appended to the `x-amz-trailer` header.

### Pre-Calculated Checksum
The user may pre-calculate the checksum and provide it in the request. The SDK automatically parses this checksum 
and adds it to the request headers. When this header is present, the rest of the flexible checksums request workflow is skipped.

Note: the user must still fill in the member specified by `requestAlgorithmMember` even if the checksum itself is supplied in the request.
If the checksum header's algorithm and the checksum algorithm do not match, the pre-calculated checksum will be ignored 
and the checksum will be calculated internally. See [the appendix](#sha-1-checksum-with-ignored-precalculated-value) for an example.

### Validating Checksum Algorithms

When a user sets the `requestAlgorithmMember` property, they are opting-in to sending request checksums. 

This property is modeled as an enum value, so validation needs to be done prior to using it. The enum is generated from the service model,
but the set of possible enum values is constrained by the [`httpChecksum` trait specification](#checksum-algorithms). The following code will match a `String` to a [`HashFunction`](https://github.com/awslabs/smithy-kotlin/blob/5773afb348c779b9e4aa9689836844f21a571908/runtime/hashing/common/src/aws/smithy/kotlin/runtime/hashing/HashFunction.kt).
```kotlin
public fun String.toHashFunction(): HashFunction? {
    return when (this.lowercase()) {
        "crc32" -> Crc32()
        "crc32c" -> Crc32c()
        "sha1" -> Sha1()
        "sha256" -> Sha256()
        "md5" -> Md5()
        else -> return null
    }
}
```
Note that `Md5` is included here, but it is not a supported flexible checksum algorithm.

There is a secondary validation to ensure that the user-specified algorithm is allowed to be used in flexible checksums:
```kotlin
private val HashFunction.isSupported: Boolean get() = when (this) {
    is Crc32, is Crc32c, is Sha256, is Sha1 -> true
    else -> false
}
```

An exception will be thrown if the algorithm can't be parsed or if it's not supported for flexible checksums. 
Note that because users select an algorithm from a code-generated enum, accidentally providing an unsupported algorithm is unlikely.

### Computing and Injecting Checksums
Next the SDK will compute and inject the checksum. If the body is smaller than the aws-chunked threshold ([1MB today](https://github.com/awslabs/smithy-kotlin/blob/9b9297c690d9a01777447f437f0e91562e146bf9/runtime/auth/aws-signing-common/common/src/aws/smithy/kotlin/runtime/auth/awssigning/middleware/AwsSigningMiddleware.kt#L38)), 
the checksum will be immediately computed and injected under the appropriate header name.

Otherwise, if the request body is large enough to be uploaded with `aws-chunked`, the SDK will set the `x-amz-trailer` header
to the checksum header name. 

For example, if the user is uploading an aws-chunked body and using the `CRC32C` checksum algorithm, the request will look like:
```
PUT SOMEURL HTTP/1.1
x-amz-trailer: x-amz-checksum-crc32c
x-amz-content-sha256: STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER
Content-Encoding: aws-chunked
Content-Length: 1238
...

400;chunk-signature=<chunk-signature> + \r\n + [1024 bytes of payload data] + \r\n
0;chunk-signature=<signature> + \r\n
x-amz-checksum-crc32c:AAAAAA== + \r\n
x-amz-trailer-signature:<trailer-signature> + \r\n
\r\n
```

To calculate the checksum while the payload is being written, the body will be wrapped in either a `HashingSource`
or a `HashingByteReadChannel`, depending on its type. These are new types which are constructed with an [`SdkSource`](https://github.com/awslabs/smithy-kotlin/blob/5773afb348c779b9e4aa9689836844f21a571908/runtime/io/common/src/aws/smithy/kotlin/runtime/io/SdkSource.kt) or 
[`SdkByteReadChannel`](https://github.com/awslabs/smithy-kotlin/blob/5773afb348c779b9e4aa9689836844f21a571908/runtime/io/common/src/aws/smithy/kotlin/runtime/io/SdkByteReadChannel.kt),
respectively, along with a `HashFunction`. These constructs will use the provided hash function 
to compute the checksum as the data is being read.

Further down the middleware chain this hashing body will be wrapped once more, in an `aws-chunked` body. This body is used to format the 
underlying data source into `aws-chunked` content encoding.

Today the [`AwsChunkedReader`](https://github.com/awslabs/smithy-kotlin/blob/5773afb348c779b9e4aa9689836844f21a571908/runtime/auth/aws-signing-common/common/src/aws/smithy/kotlin/runtime/auth/awssigning/internal/AwsChunkedReader.kt)
has the following signature:
```kotlin
AwsChunkedReader(
    private val stream: Stream,
    private val signer: AwsSigner,
    private val signingConfig: AwsSigningConfig,
    private var previousSignature: ByteArray,
    private var trailingHeaders: Headers = Headers.Empty
)
```
Note that the trailing headers are provided as a `Headers` object, which is essentially a key-value map of header names to their values.

After sending the body, the checksum needs to be sent as a trailing header.

However, it is wise to avoid tight coupling of the `aws-chunked` and flexible checksums features. The `aws-chunked` body should have
no knowledge of the `HashingSource`/`HashingByteReadChannel` it is reading from. 

So, how will the value of the checksum be passed to the `AwsChunkedReader`?

#### Lazy Headers
A concept of deferred, or "lazy" header values is introduced. At initialization, the `aws-chunked` body needs to know that
a trailing header *will be sent*, but the value can't be ready until the body has been fully consumed.

[`LazyAsyncValue`](https://github.com/awslabs/smithy-kotlin/blob/5773afb348c779b9e4aa9689836844f21a571908/runtime/utils/common/src/aws/smithy/kotlin/runtime/util/LazyAsyncValue.kt)
is the SDK's pre-existing wrapper around Kotlin's [Lazy type](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-lazy/) 
that allows asynchronous initialization.

After calling `get()` on these header values, the underlying block is executed and a value is returned.

Since the `AwsChunkedReader` will only call `get()` on the trailing headers after the body has been sent, the checksum will already
be computed and ready to retrieve from the `LazyAsyncValue`.

The `aws-chunked` trailing headers implementation is refactored to use a new `LazyHeaders` class, 
which maps `String` -> `List<LazyAsyncValue<String>>`. Below is the new signature of `AwsChunkedReader`:

```kotlin
AwsChunkedReader(
    private val stream: Stream,
    private val signer: AwsSigner,
    private val signingConfig: AwsSigningConfig,
    private var previousSignature: ByteArray,
    private var trailingHeaders: LazyHeaders = LazyHeaders.Empty
)
```

#### Passing Around Trailing Headers

There are many places where a user or developer may want to mutate the trailing headers. To make the trailing headers
easily mutable, the builder will be stored in the `ExecutionContext` with the key `TrailingHeaders`. This allows users 
to fetch, mutate, and store the trailing headers at any point before the request is signed. The list of trailing header names
must be included as a signed header, so trailing headers cannot be added after signing is done. 

## Responses

After making a request, a user may want to validate the response using a checksum. Users can opt-in to validating response checksums by setting a non-null `requestValidationModeMember`.

### Checksum Validation Priority
The service may return many checksums, but the SDK must only validate one.

When multiple checksums are returned, the validation priority is: 

1. `CRC32C`
1. `CRC32` 
1. `SHA1`
1. `SHA256`

For example, if the service returns both `SHA256` and `CRC32` checksums, the SDK must only validate the `CRC32` checksum.


### Middleware

To run this validation process, a new middleware is inserted at the `receive` stage. During an HTTP request lifecycle,
this stage represents the first opportunity to access the response prior to deserialization into the operation's response type.

If the request has a non-streaming response, the middleware will compute and validate the response checksum in a blocking
manner. If there is a checksum mismatch, an exception will be thrown.

#### Deferred Checksum Validation

For streaming responses, it is best for the SDK to compute the checksum as the user is consuming the response body. 

The `receive` stage is run prior to the user consuming the body, so while in this stage, the SDK will wrap the response
body in a hashing body, in a similar manner to the request middleware. The execution context will be updated with:

- expected checksum: the checksum value from response headers
- calculated checksum: a `LazyAsyncValue` containing the calculated checksum, which will only be fetched after the entire response body is consumed
- checksum header name: the name of the checksum header to be validated, which allows the user to see if validation 
occurred and which checksum algorithm was used.

The checksum will be validated after the response is fully consumed by the user. 

#### Notifying the User of Validation

In some cases, a service will not return a checksum even if it is requested. Because of this, the SDK must provide a mechanism for users to verify whether checksum validation occurred, 
and which checksum algorithm was used for the validation.

The SDK will store the checksum header name in the execution context. Users can then check the execution context for that 
value, and if it's present, they will know that validation occurred.

Users will be able to observe the execution context using an [interceptor](https://github.com/awslabs/smithy-kotlin/blob/5773afb348c779b9e4aa9689836844f21a571908/docs/design/interceptors.md). 

# Appendix: Request Examples
In the following examples, `requestAlgorithMember` property's value is `checksumAlgorithm`. The fields `checksumSha256`, `checksumCrc32`, etc. 
are modeled individually on the operation, separately from the `httpChecksum` trait.

## CRC32C Checksum
```kotlin
val putObjectRequest = PutObjectRequest {
    bucket = "bucket"
    key = "key"
    checksumAlgorithm = ChecksumAlgorithm.CRC32C
}
```

## SHA-256 Checksum with Precalculated Value
```kotlin
val putObjectRequest = PutObjectRequest {
    bucket = "bucket"
    key = "key"
    checksumAlgorithm = ChecksumAlgorithm.SHA256
    checksumSha256 = "checksum"
}
```

## SHA-1 Checksum with Ignored Precalculated Value
The following request will have its pre-calculated checksum ignored, since it does not match the chosen checksum algorithm.
```kotlin
val putObjectRequest = PutObjectRequest {
    bucket = "bucket"
    key = "key"
    checksumAlgorithm = ChecksumAlgorithm.SHA1
    checksumCrc32 = "checksum" // ignored
}
```

## Providing only the Precalculated Value is Invalid
The following request will not run any flexible checksums processes, because no checksum algorithm was specified.
```kotlin
val putObjectRequest = PutObjectRequest {
    bucket = "bucket"
    key = "key"
    checksumCrc32 = "checksum"
}
```

# Appendix: Response Examples
In the following example, the `requestValidationModeMember` property's value is `checksumMode`.

## Opting-In to Response Validation
```kotlin
val getObjectRequest = GetObjectRequest {
    bucket = "bucket"
    key = "key"
    checksumMode = ChecksumMode.Enabled
}
```

# Appendix: Alternative Designs Considered

## CompletableFuture Deferred Headers
Instead of `LazyAsyncValue`, `CompletableFuture` / `Future` were evaluated for use as a deferred header value.

`LazyAsyncValue` was ultimately chosen because:
- it is already used in other similar places by the SDK
  - it's better to reuse something that already exists
- `CompletableFuture` uses coroutines, which adds unnecessary cognitive load for developers

## Storing Trailing Headers in `HttpRequest` or `HttpBody` 
It is useful to allow users to modify the trailing headers at any point before the request is signed.
The accepted design choice is to store trailing headers in the `ExecutionContext`, where they can be retrieved at any point.

Alternatively, the `HttpRequest` or `HttpBody` could have stored these trailing headers.

### HttpRequest
Specifically, this would have involved modifying [`HttpRequestBuilder`](https://github.com/awslabs/smithy-kotlin/blob/5773afb348c779b9e4aa9689836844f21a571908/runtime/protocol/http/common/src/aws/smithy/kotlin/runtime/http/request/HttpRequestBuilder.kt).
We would add a field to the constructor which will contain the trailing headers to be sent.

Pros:
- `headers` are already stored here, it is a logical place for `trailingHeaders` too
- More robust access method than `ExecutionContext`

Cons:
- `headers` are used in every HTTP request, but `trailingHeaders` will only be used for a subset of requests
  - Extra bloat in the class

### HttpBody

By adding the trailingHeaders to [HttpBody](https://github.com/awslabs/smithy-kotlin/blob/5773afb348c779b9e4aa9689836844f21a571908/runtime/protocol/http/common/src/aws/smithy/kotlin/runtime/http/HttpBody.kt), 
users can modify the trailing headers anywhere they have access to the request body.

Pros:
- More robust access method than `ExecutionContext`

Cons:
- Trailing headers don't "fit" here: `HttpBody` just has `contentLength`, `isOneShot`, and `isDuplex`. These all relate to the content of the body,
and headers don't really fit in here.

## Blocking Checksum Validation
Initially, instead of calculating the response checksum as the body is consumed by the user, 
calculation was being done in a blocking manner in the middleware.
The response body was being hashed and validated prior to passing the response on to the user.

There are pros and cons to this design choice, but ultimately it was decided against blocking to calculate the checksum.

Pros:
- The checksum is calculated prior to passing the response to the user
  - With the accepted design choice, if the checksum is invalid, the user will only know about it after they've consumed
    the whole response body. If they are sending the response body downstream, for example, they would then have to cancel,
    invalidate, or otherwise handle the invalid body
  - This proposed design choice would allow the SDK to throw an exception on an invalid checksum *before* giving it to
    the user

Cons:
- Double-read of the response body (once to calculate checksum and once by the user)
- May require buffering in-memory or even in-disk for very large responses

Ultimately, it was decided that because invalid checksums are unlikely to occur often, the design flaw around checksum invalidation
is less of a worry than other cons such as double-reads of the response body.

# Revision history
- 10/24/2022 - Created
- 12/21/2022 - Updated with references to `aws-chunked`
