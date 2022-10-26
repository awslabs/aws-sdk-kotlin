# Flexible Checksums Design

* **Type**: Design
* **Author**: Matas Lauzadis

# Abstract

[Flexible checksums](https://aws.amazon.com/blogs/aws/new-additional-checksum-algorithms-for-amazon-s3/) is a feature 
that allows users and services to configure checksum operations for both HTTP requests and responses. To enable the feature, 
services add an `httpChecksum` trait to their Smithy models.

This document covers the design for supporting flexible checksums in the AWS SDK for Kotlin. 

# Design

## Requirements



- Support the Smithy trait `httpChecksum`
- Implement CRC32C
- Deprecate `httpChecksumRequired`

## `httpChecksum` Trait

Services may use the `httpChecksum` trait in their Smithy models to define flexible checksums behavior.
There are four properties in this trait:
- `requestChecksumRequired` if a checksum is required for the HTTP request
- `requestAlgorithmMember` the opt-in status for sending request checksums (a non-null value means "enabled")
- `requestValidationModeMember` the opt-in status for validating checksums in the HTTP response
- `responseAlgorithms` a list of strings representing algorithms that must be used for checksum validation

### Deprecating `httpChecksumRequired`

The `httpChecksumRequired` Smithy trait is now deprecated. We need to use the `httpChecksum` trait's
`requestChecksumRequired` property instead.

Previously, when `httpChecksumRequired` was set to `true`, we would compute the checksum using MD5 and inject it
into the `Content-MD5` header.

If the `requestChecksumRequired` property is set to `true`, and the customer opts-in to using flexible checksums,
we must give priority to the flexible checksums implementation. Otherwise if not opted-in, we must continue the previous
behavior of injecting the `Content-MD5` header.

## Checksum Algorithms

We need to support the following checksum algorithms: CRC32C, CRC32, SHA1, SHA256

All of them are [already implemented for JVM](https://github.com/awslabs/smithy-kotlin/tree/main/runtime/hashing/jvm/src/aws/smithy/kotlin/runtime/hashing)
__except for CRC32C__. This algorithm is essentially the same as CRC32, but uses a different polynomial under the hood.

We use [java.util.zip to import CRC32](https://docs.oracle.com/javase/8/docs/api/java/util/zip/CRC32.html), but this package 
only began supporting CRC32C in Java 9. We want to support Java 8 at a minimum, so we will need to implement this 
ourselves (which is [what the Java SDK does](https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/checksums/factory/SdkCrc32C.java)).

## Checksum Header Name

The checksum header name will be `x-amz-checksum-<checksum_algorithm_name>`. For example, if the checksum was computed 
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
For streaming requests which are either authorized with streaming-signing or unsigned, we need to
place the checksum in the trailer.

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

# Appendix

# Revision history
- 10/24/2022 - Created
