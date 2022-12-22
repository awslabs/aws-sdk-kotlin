# Flexible Checksums Design

* **Type**: Design
* **Author**: Matas Lauzadis

# Abstract

[Flexible checksums](https://aws.amazon.com/blogs/aws/new-additional-checksum-algorithms-for-amazon-s3/) is a feature 
that allows users and services to configure checksum validation for HTTP requests and responses. To use the feature, 
services add an `httpChecksum` trait to their Smithy models. Users may then opt-in to sending request checksums or validating
response checksums.

This document covers the design for supporting flexible checksums in the AWS SDK for Kotlin. 

# `httpChecksum` Trait

Services use the `httpChecksum` trait on their Smithy operations to enable flexible checksums.
There are four properties to this trait:
- `requestChecksumRequired` represents if a checksum is required for the HTTP request
- `requestAlgorithmMember` represents the opt-in status for sending request checksums (a non-null value means "enabled")
- `requestValidationModeMember` represents the opt-in status for validating checksums returned in the HTTP response
- `responseAlgorithms` represents a list of strings of checksum algorithms that are used for response validation

### Deprecating `httpChecksumRequired`

Before flexible checksums, services used the `httpChecksumRequired` trait to require a checksum in the request. 
This is computed using the MD5 algorithm and injected in the request under the `Content-MD5` header. 

This `httpChecksumRequired` trait is now deprecated. Services should use the `httpChecksum` trait's
`requestChecksumRequired` property instead.

If the `requestChecksumRequired` property is set to `true`, **and** the user opts-in to using flexible checksums,
the SDK must use flexible checksums. Otherwise, if a request requires a checksum but
the user has not opted-in, the SDK will continue the legacy behavior of injecting the `Content-MD5` header.

## Checksum Algorithms

We need to support the following checksum algorithms: CRC32C, CRC32, SHA1, SHA256

All of them are [already implemented for JVM](https://github.com/awslabs/smithy-kotlin/tree/main/runtime/hashing/jvm/src/aws/smithy/kotlin/runtime/hashing)
~~**except for CRC32C**~~. This algorithm is essentially the same as CRC32, but uses a different polynomial under the hood.
  The SDK uses [java.util.zip's implementation of CRC32](https://docs.oracle.com/javase/8/docs/api/java/util/zip/CRC32.html), but this package 
only began shipping CRC32C in Java 9. The SDK wants to support Java 8 at a minimum, and so will need to implement this rather than using a dependency
(which is also [what the Java SDK does](https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/checksums/factory/SdkCrc32C.java)).

Note: CRC32C was implemented in **smithy-kotlin** [PR#724](https://github.com/awslabs/smithy-kotlin/pull/724).

## Checksum Header Name

The header name used to set the checksum value is `x-amz-checksum-<checksum_algorithm_name>`. For example, if the checksum was computed 
using SHA-256, the header containing the checksum will be `x-amz-checksum-sha256`.

# Design

This feature can be broken up into two new middleware -- one for calculating checksums for requests, and one for
validating checksums present in responses.

# Requests

## Overview
During an HTTP request, the SDK first needs to check if the user has opted-in to sending checksums. If they have not opted-in,
but the operation has the `requestChecksumRequired` property set, the SDK will fall back to the legacy behavior of computing the MD5 checksum.

## Middleware
A new middleware is introduced at the `mutate` stage. This stage of the HTTP request lifecycle represents the last chance
to modify the request before it is sent on the network.

There are many middleware which operate at the `mutate` stage. It is important that this new middleware come before 
[AwsSigningMiddleware](https://github.com/awslabs/smithy-kotlin/blob/main/runtime/auth/aws-signing-common/common/src/aws/smithy/kotlin/runtime/auth/awssigning/middleware/AwsSigningMiddleware.kt#L26)
because that middleware is dependent on the header values set in this new middleware.

The SDK exposes an `order` integer parameter (defaulted to 0) which is used to model dependencies between middleware.
The `order` of AwsSigningMiddleware has already been set to 126, which ensures it will be executed towards the end of the mutate middleware stack,
after this flexible checksums middleware has run.

## Normal vs. Streaming Requests

The request checksum should be sent as either as a header or trailing header. The following table lays out all the possible cases
of where the checksum should be placed.

| Payload Type | Authorization Type | Location of Checksum |
|--------------|--------------------|----------------------|
| Normal       | Header-based       | Header               |
| Normal       | Unsigned           | Header               |
| Streaming    | Header-based       | Header               |
| Streaming    | Streaming-signing  | Trailer              |
| Streaming    | Unsigned           | Trailer              |

### Normal Requests
For all normal requests, the checksum should be injected into the header.

### Streaming Requests
For streaming requests which are either streaming-signing or unsigned, the checksum must be sent as a trailing header via `aws-chunked` encoding.

To indicate that a trailing header will be sent, the SDK sets the `x-amz-trailer` header to a String of comma-delimited trailing header names.
The service uses this header to parse the trailing headers that are sent later.

For flexible checksums, we append the [checksum header name](#checksum-header-name) to the `x-amz-trailer` header.

## Input Checksum
The user may pre-calculate the checksum and provide it as input. The SDK automatically parses this input
and adds it to the request headers. When this header is present, the rest of the flexible checksums request workflow is skipped.

Note: the user must still specify the `ChecksumAlgorithm` even if the checksum itself is supplied as input.
If the input checksum and checksum algorithm do not match, the input checksum will be ignored and the checksum will be calculated
internally.

## Validating Input Algorithms

When a user sets the `requestAlgorithmMember` property, they are choosing to opt-in to sending request checksums. 

This is modeled as an enum value, so validation needs to be done prior to using it. The following code will match a String input to a HashFunction.
```kotlin
public fun String.toHashFunction(): HashFunction {
    return when (this.lowercase()) {
        "crc32" -> Crc32()
        "crc32c" -> Crc32c()
        "sha1" -> Sha1()
        "sha256" -> Sha256()
        "md5" -> Md5()
        else -> throw RuntimeException("$this is not a supported hash function")
    }
}
```
Note that MD5 is included here, but it is not a supported flexible checksum algorithm.

There is a secondary validation to ensure that the user-specified algorithm is allowed to be used in flexible checksums:
```kotlin
private val HashFunction.isSupported: Boolean get() = when (this) {
    is Crc32, is Crc32c, is Sha256, is Sha1 -> true
    else -> false
}
```

An exception will be thrown if the algorithm can't be parsed or if it's not supported for flexible checksums. 
Note that users select an algorithm from a code-generated enum, so accidentally providing an unsupported algorithm is unlikely.


## Computing and Injecting Checksums
Next the SDK will compute and inject the checksum. If the body is smaller than the aws-chunked threshold (1MB today), 
the checksum will be immediately computed and injected under the appropriate header name.

## aws-chunked
Otherwise, if the request body is large enough to be uploaded with `aws-chunked`, the SDK will set the `x-amz-trailer` header
to the checksum header name. 

For example, if the user is uploading an aws-chunked body and using the CRC32C checksum algorithm, the request will look like:
```
> PUT SOMEURL HTTP/1.1
> x-amz-trailer: x-amz-checksum-crc32c
> x-amz-content-sha256: STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER
> Content-Encoding: aws-chunked
> Content-Length: 1238
> ...
>
> 400;chunk-signature=<chunk-signature> + \r\n + [1024 bytes of payload data] + \r\n
> 0;chunk-signature=<signature> + \r\n
> x-amz-checksum-crc32c:AAAAAA== + \r\n
> x-amz-trailer-signature:<trailer-signature> + \r\n
> \r\n
```

To calculate the checksum while the payload is being written, the body will be wrapped in either a HashingSource
or a HashingByteReadChannel, depending on its type.

These constructs will use the provided checksum algorithm to compute the checksum as the data is being read.

Further down the middleware chain, this hashing body will be wrapped once more in an aws-chunked body. This body is used to format the 
underlying data source into aws-chunked content encoding.

Today the aws-chunked reader has the following signature:
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

However, it is wise to avoid tight coupling of the aws-chunked and flexible checksums features. The aws-chunked body should have
no knowledge of the HashingSource/HashingByteReadChannel it is reading from. 

So, how will the value of the checksum be passed to the AwsChunked body?

### Lazy Headers
A concept of deferred, or "lazy" header values is introduced. At initialization, the aws-chunked body needs to know that
a trailing header *will be sent*, but the value can't be ready until the body has been fully consumed.

LazyAsyncValue is the SDK's pre-existing wrapper around Kotlin's [Lazy type](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-lazy/) 
that allows asynchronous initialization.

After calling `get()` on these header values, the underlying block is executed and a value is returned.

Since the AwsChunked body will only call `get()` on the trailing headers after the body has been sent, the checksum will already
be computed and ready to retrieve from the LazyAsyncValue.

The aws-chunked trailing headers implementation is refactored to use the new `LazyHeaders` class, 
which maps String -> List<LazyAsyncValue&lt;String>>

```kotlin
AwsChunkedReader(
    private val stream: Stream,
    private val signer: AwsSigner,
    private val signingConfig: AwsSigningConfig,
    private var previousSignature: ByteArray,
    private var trailingHeaders: LazyHeaders = LazyHeaders.Empty
)
```

# Responses

After making a request, a user may want to validate the response using a checksum.

Users can opt-in to validating response checksums by setting a non-null `requestValidationModeMember`.

## Checksum Validation Priority
The service may return many checksums, but the SDK must only validate one.

When multiple checksums are returned, the validation priority is: 

1. CRC32C
1. CRC32 
1. SHA1
1. SHA256

For example, if the service returns both SHA256 and CRC32 checksums, the SDK must only validate the CRC32 checksum.


## Middleware

To run this validation process, a new middleware is inserted at the `receive` stage. During an HTTP request lifecycle,
this stage represents the first opportunity to access the response prior to deserialization into the operation's Response type.

### Rolling Hash

It is important to calculate the checksum in a rolling manner. The SDK can't read the entire response body into memory,
as this may cause users' machines to run out of memory.

### Notifying the User

In some cases, a service will not return a checksum even if it is requested.

Because of this, the SDK must provide a mechanism for users to verify whether checksum validation occurred, 
and which checksum algorithm was used for the validation.

// TODO We can store this in the execution context, which can then be read by the user. (how?)

// TODO interceptors?

# Appendix

## Request Examples

### CRC32C Checksum
```kotlin
val putObjectRequest = PutObjectRequest {
    bucket = "bucket"
    key = "key"
    checksumAlgorithm = ChecksumAlgorithm.CRC32C
}
```

### SHA256 Checksum with Precalculated Value
```kotlin
val putObjectRequest = PutObjectRequest {
    bucket = "bucket"
    key = "key"
    checksumAlgorithm = ChecksumAlgorithm.SHA256
    checksumSha256 = "checksum"
}
```

### SHA1 Checksum with Ignored Precalculated Value
The following request will have its pre-calculated checksum ignored, since it does not match the checksum algorithm specified.
```kotlin
val putObjectRequest = PutObjectRequest {
    bucket = "bucket"
    key = "key"
    checksumAlgorithm = ChecksumAlgorithm.SHA1
    checksumCrc32 = "checksum" // ignored
}
```

### Providing only the Precalculated Value is Invalid
The following request will not run any flexible checksums workflow, because no checksum algorithm was specified.

```kotlin
val putObjectRequest = PutObjectRequest {
    bucket = "bucket"
    key = "key"
    checksumCrc32 = "checksum"
}
```

## Response Examples

### Opting-In to Response Validation
```kotlin
val getObjectRequest = GetObjectRequest {
    bucket = "bucket"
    key = "key"
    checksumMode = ChecksumMode.Enabled
}
```

# Revision history
- 10/24/2022 - Created
- 12/21/2022 - Updated with references to `aws-chunked`
