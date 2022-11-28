# Flexible Checksums Design

* **Type**: Design
* **Author**: Matas Lauzadis

# Abstract

[Flexible checksums](https://aws.amazon.com/blogs/aws/new-additional-checksum-algorithms-for-amazon-s3/) is a feature 
that allows users and services to configure checksum operations for HTTP requests and responses. To enable the feature, 
services add an `httpChecksum` trait to their Smithy models.

This document covers the design for supporting flexible checksums in the AWS SDK for Kotlin. 

# `httpChecksum` Trait

Services may use the `httpChecksum` trait in their Smithy models to define flexible checksums behavior.
There are four properties in this trait:
- `requestChecksumRequired` if a checksum is required for the HTTP request
- `requestAlgorithmMember` the opt-in status for sending request checksums (a non-null value means "enabled")
- `requestValidationModeMember` the opt-in status for validating checksums in the HTTP response
- `responseAlgorithms` a list of strings representing algorithms that must be used for checksum validation

### Deprecating `httpChecksumRequired`

The `httpChecksumRequired` Smithy trait is now deprecated. Instead, the SDK should use the `httpChecksum` trait's
`requestChecksumRequired` property instead.

Previously, when `httpChecksumRequired` was set to `true`, the SDK would compute the MD5 checksum and set the 
result in the `Content-MD5` header.

If the `requestChecksumRequired` property is set to `true`, and the user opts-in to using flexible checksums,
the SDK must give priority to the flexible checksums implementation. Otherwise if not opted-in, we must continue the previous
behavior of injecting the `Content-MD5` header.

## Checksum Algorithms

We need to support the following checksum algorithms: CRC32C, CRC32, SHA1, SHA256

All of them are [already implemented for JVM](https://github.com/awslabs/smithy-kotlin/tree/main/runtime/hashing/jvm/src/aws/smithy/kotlin/runtime/hashing)
__except for CRC32C__. This algorithm is essentially the same as CRC32, but uses a different polynomial under the hood.
  The SDK uses [java.util.zip's implementation of CRC32](https://docs.oracle.com/javase/8/docs/api/java/util/zip/CRC32.html), but this package 
only began shipping CRC32C in Java 9. The SDK wants to support Java 8 at a minimum, and will need to implement this 
ourselves (which is also [what the Java SDK does](https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/checksums/factory/SdkCrc32C.java)).

## Checksum Header Name

The header name used to provide the checksum value is `x-amz-checksum-<checksum_algorithm_name>`. For example, if the checksum was computed 
using SHA-256, the header containing the checksum will be `x-amz-checksum-sha256`.

## Normal vs. Streaming Requests

The checksum should be placed in the HTTP request either as a header or trailer. The following table lays out all the possible cases
and where the request checksum should be placed.

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
For streaming requests which are either streaming-signing or unsigned, the checksum must be sent as a trailing header.

- The `Content-Encoding` header __MUST__ be set to `aws-chunked`.
- The `x-amz-trailer` trailing header __MUST__ be set to the [checksum header name](#checksum-header-name).
- For S3 operations, we __MUST__ set the `x-amz-decoded-content-length` header to the original payload size.
- We _may_ set the `transfer-encoding` header to `chunked` instead of setting the `Content-Length` header

#### Unsigned Streaming Requests

For unsigned streaming requests, we need to set one more header.

- The `x-amz-content-sha256` header __MUST__ be set to `STREAMING-UNSIGNED-PAYLOAD-TRAILER`.
  - The Authorization header computation __MUST__ use `STREAMING-UNSIGNED-PAYLOAD-TRAILER` instead of `UNSIGNED-PAYLOAD`.

## Validating Responses

Responses always store the checksum in the header. When the `httpChecksum` trait's `requestValidationModeMember` property is set to any
non-null value, we __MUST__ validate the checksum in the HTTP response.

### Checksum Validation List
The service may return many checksums. We __MUST__ only validate one. The validation priority is: CRC32C, CRC32, SHA1, SHA256.

For example, if we receive SHA256 and CRC32 checksums, we **MUST** only validate the CRC32 checksum.

### Validation Process
1. Check the `responseAlgorithms` property to find a list of checksum algorithms that should be validated. 
Set `validationList = validationList ∩ responseAlgorithms`. (We take the intersection because we don't want to try 
validating a checksum that could not possibly be returned)
1. Send the request
1. Get a response
1. Find the first algorithm in the validation list which appears in the response's headers
1. Compute and validate the checksum. If validation fails, throw a ChecksumMismatch error

We __MUST__ provide a mechanism for customers to verify whether a checksum validation occurred, and which checksum algorithm was used.
It is recommended to do this by setting the response metadata, or storing it in some context object.

We __MUST__ validate only one checksum, even if the service sends many.

# Design

All of the desired behavior can be accomplished by adding new middleware.

## Requests

During an HTTP request, we need to check if the user has opted-in to sending checksums, and if so, calculate the checksum using 
the algorithm they specified, and inject it into either the [header or trailer](#normal-vs-streaming-requests).

### Validating Input Algorithms

When a user sets the `requestAlgorithmMember` property, they are choosing to opt-in to sending request checksums. 

This comes in as a string, so we need to do some validation. TODO / QUESTION can we codegen a user-accessible enum of these algorithm names? From the specification:
> This opt-in input member MUST target an algorithm enum representing the algorithm name for which checksum value is auto-computed. And the algorithm enum values are fixed to predefined set of supported checksum algorithm names

A middleware will be placed in the [`initialize` stage](https://github.com/awslabs/smithy-kotlin/blob/cfa0fd3a30b4c50b75485786f043d4e2ad803f55/runtime/protocol/http/common/src/aws/smithy/kotlin/runtime/http/operation/SdkOperationExecution.kt#L41-L44)
which will validate the input string against the possible choices, and throw an exception if the user's specified algorithm is not supported.

```kotlin
class ValidateChecksumAlgorithmName : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = model
        .shapes<OperationShape>()
        .any { it.hasTrait<HttpChecksumTrait>() }

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> {
        return resolved + ValidateChecksumAlgorithmNameMiddleware()
    }
}

private class ValidateChecksumAlgorithmNameMiddleware : ProtocolMiddleware {
  override val name: String
    get() = "ValidateChecksumAlgorithmNameMiddleware"

  override val order: Byte = -127

  override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean {
    return op.input.getOrNull()?.let {
      ctx.model.shapes<OperationShape>().any {
        return if (it.hasTrait<HttpChecksumTrait>()) {
          val algorithmName = it.getTrait<HttpChecksumTrait>()?.requestAlgorithmMember?.getOrNull()
          algorithmName?.let { name -> return if (isValidAlgorithmName(name)) true else throw Exception("invalid checksum algorithm name $name") }
            ?: false
        } else false
      }
    } ?: false
  }

  fun isValidAlgorithmName(algorithmName: String): Boolean =
    HttpChecksumTrait.CHECKSUM_ALGORITHMS.any { it.equals(name, ignoreCase = true) }

  override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
    writer.addImport(RuntimeTypes.Http.Operation.OperationRequest)

    writer.withBlock("op.execution.initialize.intercept { req, next -> ", "}") {
      write("next.call(req)")
    }
  }
}
```

## Computing and Injecting Checksums

After validating the algorithm name, we can compute and inject the checksum. This should be done in the [mutate stage](https://github.com/awslabs/smithy-kotlin/blob/cfa0fd3a30b4c50b75485786f043d4e2ad803f55/runtime/protocol/http/common/src/aws/smithy/kotlin/runtime/http/operation/SdkOperationExecution.kt#L46-L51).

At this point, we can check if the request is a streaming request, and set the checksum in the trailer if so.

## Validating Response Checksums

Services can require validation of responses by setting a non-null `requestValidationModeMember` property.
The response checksums will always be stored in the header. See [Validation Process](#validation-process) for the full details.

We can create this middleware at the [receive stage](https://github.com/awslabs/smithy-kotlin/blob/cfa0fd3a30b4c50b75485786f043d4e2ad803f55/runtime/protocol/http/common/src/aws/smithy/kotlin/runtime/http/operation/SdkOperationExecution.kt#L59-L62)
because this is the first opportunity to access the response data before deserialization.

# Appendix

# Revision history
- 10/24/2022 - Created
