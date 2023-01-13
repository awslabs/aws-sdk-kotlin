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

Before flexible checksums, services used the [`httpChecksumRequired` trait](https://smithy.io/2.0/spec/http-bindings.html#httpchecksumrequired-trait) to model a required checksum in the request. 
This was computed using the MD5 algorithm and injected in the request under the `Content-MD5` header. The `httpChecksumRequired` trait is now deprecated. 
AWS services should use the `httpChecksum` trait's `requestChecksumRequired` property instead.

The `requestChecksumRequired` property being set to `true` or the `httpChecksumRequired` trait being present on an operation
means a checksum is required for that operation.

If a checksum is required, and the user does not opt-in to using flexible checksums, the SDK will continue the legacy behavior
of injecting the `Content-MD5` header.
## Checksum Algorithms

The SDK needs to support the following checksum algorithms: CRC32C, CRC32, SHA-1, SHA-256.
All of them are [already implemented for JVM](https://github.com/awslabs/smithy-kotlin/tree/5773afb348c779b9e4aa9689836844f21a571908/runtime/hashing/jvm/src/aws/smithy/kotlin/runtime/hashing).

As part of this feature, CRC32C was implemented in **smithy-kotlin** [PR#724](https://github.com/awslabs/smithy-kotlin/pull/724). 
This algorithm is essentially the same as CRC32, but uses a different polynomial under the hood. 
The SDK uses [`java.util.zip`'s implementation of CRC32](https://docs.oracle.com/javase/8/docs/api/java/util/zip/CRC32.html), 
but this package only began shipping CRC32C in Java 9. The SDK requires Java 8, so this was implemented
rather than imported as a dependency (which is also [what the Java SDK did](https://github.com/aws/aws-sdk-java-v2/blob/ecc12b43a4aedc433c39742a2ae1361bd8d17991/core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/checksums/factory/SdkCrc32C.java)).

### Header Name

The header name used to set the checksum value is `x-amz-checksum-<checksum_algorithm_name>`. For example, if the checksum was computed 
using SHA-256, the header containing the checksum will be `x-amz-checksum-sha256`.

# Implementation

This feature can be implemented by adding two new middleware: one for calculating checksums for requests, and one for
validating checksums present in responses.

## Requests

During an HTTP request, the SDK first needs to check if the user has opted-in to sending checksums. If they have not opted-in,
but the operation has the `requestChecksumRequired` property set, the SDK will fall back to the legacy behavior of computing the MD5 checksum.

### Middleware
A new middleware is introduced at the `mutate` stage. There are many middleware which operate at this stage. 
It is important that this new middleware come before [`AwsSigningMiddleware`](https://github.com/awslabs/smithy-kotlin/blob/5773afb348c779b9e4aa9689836844f21a571908/runtime/auth/aws-signing-common/common/src/aws/smithy/kotlin/runtime/auth/awssigning/middleware/AwsSigningMiddleware.kt#L26)
because it is dependent on the header values set in this new middleware (specifically `x-amz-trailer`).

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
The service uses this header to parse the trailing headers which are sent later.

For flexible checksums, the [checksum header name](#header-name) will be appended to the `x-amz-trailer` header.

### Pre-Calculated Checksum
The user may pre-calculate the checksum and provide it in the request. The SDK automatically parses this checksum 
and adds it to the request headers. When any checksum headers are present, the flexible checksums request workflow is skipped.

Note: the user must still fill in the member specified by `requestAlgorithmMember` even if the checksum itself is supplied in the request.
If the checksum header's algorithm and the checksum algorithm do not match, the pre-calculated checksum will be ignored 
and the checksum will be calculated internally using the selected checksum algorithm. See [the appendix](#sha-1-checksum-with-ignored-precalculated-value) for an example of this.

### Validating Checksum Algorithms

When a user sets the member represented by the `requestAlgorithmMember` property, they are opting-in to sending request checksums. 

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
Note that MD5 is included here, but it is not a supported flexible checksum algorithm.

There is a secondary validation to ensure that the user-specified `HashFunction` is allowed to be used in flexible checksums:
```kotlin
private val HashFunction.isSupported: Boolean get() = when (this) {
    is Crc32, is Crc32c, is Sha256, is Sha1 -> true
    else -> false
}
```

An exception will be thrown if the algorithm can't be parsed or if it's not supported for flexible checksums. 
Note that because users select an algorithm from a code-generated enum, accidentally selecting an unsupported algorithm is unlikely.

### Computing and Injecting Checksums
Next the SDK will compute and inject the checksum. If the body is smaller than the `aws-chunked` threshold ([1MB today](https://github.com/awslabs/smithy-kotlin/blob/9b9297c690d9a01777447f437f0e91562e146bf9/runtime/auth/aws-signing-common/common/src/aws/smithy/kotlin/runtime/auth/awssigning/middleware/AwsSigningMiddleware.kt#L38)) 
and replayable, the checksum will be immediately computed and injected under the appropriate header name.

Otherwise, if the request body is large enough to be uploaded with `aws-chunked`, the SDK will append the checksum header name to the `x-amz-trailer` header.

For example, if the user is uploading an `aws-chunked` body and using the CRC32C checksum algorithm, the request will look like:
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
respectively, along with a `HashFunction`. These constructs will use the provided hash function to compute the checksum as the data is being read.

Further down the middleware chain, this hashing body will be wrapped once more in an `aws-chunked` body. This body is used to format the 
underlying data source into `aws-chunked` content encoding. 

After sending the body, the checksum needs to be sent as a trailing header. It's desirable to avoid tight coupling of the 
`aws-chunked` and flexible checksums features. The `aws-chunked` body should have no knowledge of the `HashingSource`/`HashingByteReadChannel` it's reading from. 

#### Deferred Headers
A concept of deferred header values is introduced to address this trailing header coupling issue. At initialization, the `aws-chunked` body needs to know that
a trailing header *will be sent*, but the value can't be ready until the body has been fully consumed.

Kotlin's coroutines library provides a [`Deferred` type](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-deferred/)
which is used to store a future value. [`CompletableDeferred`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-completable-deferred/)
is a subclass of `Deferred`, which allows the result of the future to be set manually. 
It's often used in cases when the result is not known when the `Deferred` is created and needs to be provided later.

The trailing header values will be modeled using `CompletableDeferred`. After calling `await()` on these `CompletableDeferred` header values, the calling thread blocks until the value is completed.

The `aws-chunked` trailing headers implementation is refactored to use a new `DeferredHeaders` class, 
which maps `String` -> `List<Deferred<String>>`. Below is the updated signature of `AwsChunkedReader`:

```kotlin
AwsChunkedReader(
    private val stream: Stream,
    private val signer: AwsSigner,
    private val signingConfig: AwsSigningConfig,
    private var previousSignature: ByteArray,
    private var trailingHeaders: DeferredHeaders = DeferredHeaders.Empty
)
```

#### Completing Sources and Channels

When using `CompletableDeferred`, the `.complete()` method must be called to mark the deferred value as complete.

Because the checksum computation [is done while the request body is being sent](#computing-and-injecting-checksums), 
new types of `Source`/`ByteReadChannel` are introduced, called `CompletingSource` and `CompletingByteReadChannel` respectively.

Below is the signature of `CompletingSource`:
```kotlin
CompletingSource(
    private val deferredChecksum: CompletableDeferred<String>,
    private val source: HashingSource
)
```

This will be used to wrap the `HashingSource`. When the source is fully exhausted, 
the calculated checksum will be digested and used to `.complete()` the `CompletableDeferred`. 
The same will be done for `HashingByteReadChannel` using a `CompletingByteReadChannel`.

#### Mutating Trailing Headers

There are many places during the request's lifecycle where trailing headers could be mutated.

The trailing headers will be added as a member in the [`HttpRequestBuilder`](https://github.com/awslabs/smithy-kotlin/blob/a250c3e3e3e54ef35990a1609fb380a91b70cf4b/runtime/protocol/http/common/src/aws/smithy/kotlin/runtime/http/request/HttpRequestBuilder.kt) 
, which is where `Headers` are already stored. This will enable the trailing headers to be modified wherever the `HttpRequest` is accessible.

The `HttpRequestBuilder` signature is updated to the following:

```kotlin
public class HttpRequestBuilder private constructor(
    public var method: HttpMethod,
    public val url: UrlBuilder,
    public val headers: HeadersBuilder,
    public var body: HttpBody,
    public val trailingHeaders: DeferredHeadersBuilder,
)
```

## Responses

After making a request, a user may want to validate the response using a checksum. Users can opt-in to validating 
response checksums by setting a non-null value on the member represented by the `requestValidationModeMember` property.

### Checksum Validation Priority
The service may return many checksums, but the SDK must only validate one.

When multiple checksums are returned, the validation priority is: 

1. CRC32C
1. CRC32
1. SHA-1
1. SHA-256

For example, if the service returns both SHA-256 and CRC32 checksums, the SDK must only validate the CRC32 checksum.


### Middleware

To run this validation process, a new middleware is inserted at the `receive` stage. During an HTTP request lifecycle,
this stage represents the first opportunity to access the response prior to deserialization into the operation's response type.

If the request has a non-streaming response, the middleware will compute and validate the response checksum in a blocking
manner. If there is a checksum mismatch, an exception will be thrown.

#### Checksum Validation
For non-streaming responses, the entire response is loaded into memory at once. The SDK will validate the checksum prior to deserializing
the response into the modeled response object.

#### Deferred Checksum Validation for Streaming Responses
For streaming responses, it is more efficient for the SDK to compute the checksum while the user is consuming the response body. 

The `receive` stage is run prior to the user consuming the body, so while in this stage, the SDK will wrap the response
body in a hashing and completing body, in a similar manner to the request middleware. The execution context will be updated with the following keys:

- `ExpectedResponseChecksum`: the checksum value from response headers
- `ResponseChecksum`: a `Deferred` containing the calculated checksum, which will only be completed after the entire response body is consumed
- `ChecksumHeaderValidated`: the name of the checksum header which was validated, which allows the user to see if validation 
occurred and which checksum algorithm was used.

The checksum will be validated after the response is fully consumed by the user. 

#### Notifying the User of Validation

In some cases, a service will not return a checksum even if it is requested. Because of this, the SDK must provide a mechanism for users to verify whether checksum validation occurred, 
and which checksum algorithm was used for the validation.

The SDK will store the checksum header name in the execution context. Users can then check the execution context for that 
value, and if it's present, they will know that validation occurred.

Users will be able to observe the execution context using an [interceptor](https://github.com/awslabs/smithy-kotlin/blob/5773afb348c779b9e4aa9689836844f21a571908/docs/design/interceptors.md). 

# Appendix: Request Examples
In the following examples, the `requestAlgorithMember` property's value is `checksumAlgorithm`. The fields `checksumSha256`, `checksumCrc32`, etc. 
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
Instead of `CompletableFuture`, `LazyAsyncValue` and `RunnableFuture` were evaluated for use as a deferred header value.

### `LazyAsyncValue`

Pros:
- Already exists in the SDK
- Lazy execution
  - The checksum future can be computed only when it is ready / needed

Cons:
- No concept of "completion"
  - There is no way to indicate that the lazy value is ready for consumption. The underlying code block will be executed 
when `.get()` is called, which could happen earlier than intended
  

### `RunnableFuture`
Pros:
- Execution can be delayed until the result is actually needed
  - Fixes the issue with eager execution in `CompletableFuture`
Cons:
- There is no concept of completion
  - Calling `.get()` on a `Future` / `RunnableFuture` will block until it's complete

Ultimately, `CompletableFuture` was chosen because it provides a way to model completion and the value can be set in a non-blocking manner.

## Storing Trailing Headers in `HttpRequest` or `HttpBody` 
Users must be able to modify the trailing headers at any point before the request is signed.
The accepted design choice is to store trailing headers in the `HttpRequest`.

Alternatively, the trailing headers could have been stored in the `HttpBody` or `ExecutionContext`.

### ExecutionContext
The SDK uses [`ExecutionContext`](https://github.com/awslabs/smithy-kotlin/blob/a250c3e3e3e54ef35990a1609fb380a91b70cf4b/runtime/runtime-core/common/src/aws/smithy/kotlin/runtime/client/ExecutionContext.kt) 
as a property bag, storing a variety of properties related to the execution of a request.

To add the trailing headers to this property bag, a new `AttributeKey` would be added. This can then be used to lookup 
the trailing headers anywhere the execution context is available. 

Pros:
- Simple to implement and use

Cons:
- Not a robust access method
  - Users need to use a specific key to lookup the trailing headers
  - Nullability of values means extra logic for default values is necessary 

### HttpBody

By adding the trailing headers to [HttpBody](https://github.com/awslabs/smithy-kotlin/blob/5773afb348c779b9e4aa9689836844f21a571908/runtime/protocol/http/common/src/aws/smithy/kotlin/runtime/http/HttpBody.kt), 
users can modify the trailing headers anywhere they have access to the request body.

Pros:
- More robust access method than `ExecutionContext`

Cons:
- Trailing headers don't "fit" here: `HttpBody` has `contentLength`, `isOneShot`, and `isDuplex`. These all relate to the content of the body,
and headers don't really fit in here

Ultimately, it was decided to store trailing headers in the `HttpRequest` (specifically, 
[`HttpRequestBuilder`](https://github.com/awslabs/smithy-kotlin/blob/a250c3e3e3e54ef35990a1609fb380a91b70cf4b/runtime/protocol/http/common/src/aws/smithy/kotlin/runtime/http/request/HttpRequestBuilder.kt)
) because that is where regular headers are already stored.

## Synchronous Checksum Validation
Instead of calculating the response checksum as the body is consumed by the user, a design choice was considered where
checksum calculation would be done in a blocking manner in the middleware. This way, the response checksum would be validated
before passing the response on to the user.

Pros:
- The checksum is calculated prior to passing the response to the user
  - With the accepted design choice, if the checksum is invalid, the user will only know about it after they've consumed
    the whole response body. If they are sending the response body downstream, for example, they would then have to cancel,
    nullify, or otherwise handle the invalid body
- Allows the SDK to throw an exception on an invalid checksum **before** giving it to
  the user

Cons:
- Double-read of the response body (once to calculate checksum, and then once by the user)
- May require buffering in-memory or even in-disk for very large responses

Ultimately, it was decided that because invalid checksums are unlikely to occur often, the design flaw around checksum invalidation
is less of a worry than other cons such as double-reads of the response body.

# Revision history
- 10/24/2022 - Created
- 12/21/2022 - Updated with references to `aws-chunked`
