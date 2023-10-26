# Changelog

## [0.33.0-beta] - 10/26/2023

### Features
* **BREAKING**: Update codegen to improve nullability of generated types.
* [#605](https://github.com/awslabs/aws-sdk-kotlin/issues/605), [#805](https://github.com/awslabs/aws-sdk-kotlin/issues/805) Publish a BOM and a Version Catalog
* Detect and automatically correct clock skew to prevent signing errors

### Fixes
* Ignore empty environment variable and system property strings when evaluating AWS credentials

### Miscellaneous
* Upgrade Kotlin to 1.9.10
* Sync AWS models and upgrade smithy-kotlin
* Upgrade Dokka to 1.9.0
* **Breaking** Removed `enableAccelerate` & `forcePathStyle` from S3 config. As well as `use_accelerate_endpoint` & `addressing_style` from AWS profile configuration
* **BREAKING**: Remove `smithy.client.request.size`, `smithy.client.response.size`, `smithy.client.retries` metrics. Rename all `smithy.client.*` metrics to `smithy.client.call.*`.
* Remove GameSparks service
* Add skeleton implementation of a second KMP target
* Added `s3_use_arn_region` & `s3_disable_multiregion_access_points` to AWS profile configuration

## [0.32.5-beta] - 10/12/2023

### Features
* [#945](https://github.com/awslabs/aws-sdk-kotlin/issues/945) Add new sources for User-Agent app id

### Miscellaneous
* Sync to the latest versions of **smithy-kotlin** and AWS service models

## [0.32.4-beta] - 10/06/2023

### Miscellaneous
* Track upstream changes that make `ByteArrayContent` and friends internal. Users should only be using `ByteStream.fromBytes()`, `ByteStream.fromString()`, and `HttpBody.fromBytes()`.

## [0.32.3-beta] - 09/28/2023

### Fixes
* [#1048](https://github.com/awslabs/aws-sdk-kotlin/issues/1048) Restore public constructor for `EcsCredentialsProvider`
* [#1044](https://github.com/awslabs/aws-sdk-kotlin/issues/1044) ignore `__type` when deserializing union for AWS JSON 1.0, AWS JSON 1.1, and AWS restJson 1

### Miscellaneous
* Generate internal-only clients with `internal` visibility
* sync AWS models and upgrade smithy kotlin

## [0.32.2-beta] - 09/15/2023

### Miscellaneous
* [#946](https://github.com/awslabs/aws-sdk-kotlin/issues/946) Refactor CredentialsProvider APIs
* Sync smithy-kotlin and AWS service models.

## [0.32.1-beta] - 09/08/2023

### Features
* [#1033](https://github.com/awslabs/aws-sdk-kotlin/issues/1033) Add `SystemPropertyCredentialsProvider` and make it first in default chain credentials provider
* Allow endpoint URL configuration via env and shared config.
* [#1000](https://github.com/awslabs/aws-sdk-kotlin/issues/1000) Add more parameters for fetching STS credentials

### Fixes
* [#935](https://github.com/awslabs/smithy-kotlin/issues/935) Fix closing an event stream causing an IllegalStateException

### Miscellaneous
* Sync AWS models and bump smithy-kotlin

## [0.32.0-beta] - 08/31/2023

### Miscellaneous
* **BREAKING**: Refactor HttpCall and HttpResponse types
* Bump **smithy-kotlin** and AWS service models to latest versions

## [0.31.0-beta] - 08/24/2023

### Features
* Support initial-request and initial-response for event streams using RPC-based protocols

### Fixes
* [#1029](https://github.com/awslabs/aws-sdk-kotlin/issues/1029) Update smithy-kotlin to 0.26.0

### Miscellaneous
* **BREAKING**: prefix generated endpoint and auth scheme providers with client name and track upstream changes
* Sync AWS models
* Refactor ClientOption to AttributeKey directly and track upstream HttpContext changes

## [0.30.1-beta] - 08/17/2023

### Miscellaneous
* Sync AWS models and bump smithy-kotlin version

## [0.30.0-beta] - 08/11/2023

### Features
* [#583](https://github.com/awslabs/aws-sdk-kotlin/issues/583) Make user-supplied region available to config resolution providers
* [#1004](https://github.com/awslabs/aws-sdk-kotlin/issues/1004) Make RegionProviderChain accept a list of RegionProvider

### Fixes
* [#194](https://github.com/awslabs/aws-sdk-kotlin/issues/194) Correctly parse and handle `GetBucketLocation` responses

### Miscellaneous
* Upgrade Kotlin to 1.8.22
* [#968](https://github.com/awslabs/aws-sdk-kotlin/issues/968) Add service-level benchmarks
* Upgrade kotlinx.coroutines to 1.7.3
* Sync AWS service models and **smithy-kotlin** to latest versions

## [0.29.1-beta] - 07/27/2023

### Features
* [#745](https://github.com/awslabs/aws-sdk-kotlin/issues/745) Validate returned content length on S3 `GetObject` responses.

### Miscellaneous
* Sync AWS models and bump smithy-kotlin version

## [0.29.0-beta] - 07/20/2023

### Features
* [#146](https://github.com/awslabs/smithy-kotlin/issues/146), [#800](https://github.com/awslabs/aws-sdk-kotlin/issues/800) Enable **Timestream Query** and **Timestream Write** service clients
* [#969](https://github.com/awslabs/aws-sdk-kotlin/issues/969) Make `region` an optional client config parameter to support multi-region use cases

### Miscellaneous
* **BREAKING**: Refactor observability API and configuration. See the [discussion](https://github.com/awslabs/aws-sdk-kotlin/discussions/981) for more information.
* Sync AWS service models.

## [0.28.2-beta] - 07/13/2023

### Fixes
* [#242](https://github.com/awslabs/aws-sdk-kotlin/issues/242) Correctly handle and throw `InvalidChangeBatch` responses from Route53

### Miscellaneous
* Sync AWS service models

## [0.28.1-beta] - 07/06/2023

### Miscellaneous
* Upgrade smithy-kotlin and sync service models.

## [0.28.0-beta] - 06/29/2023

### Features
* [#701](https://github.com/awslabs/aws-sdk-kotlin/issues/701) **Breaking**: Simplify mechanisms for setting/updating retry strategies in client config. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/964) for more details.
* [#701](https://github.com/awslabs/aws-sdk-kotlin/issues/701) Add adaptive retry mode

### Miscellaneous
* Sync AWS models and bump smithy-kotlin version

## [0.27.2-beta] - 06/22/2023

### Miscellaneous
* Sync AWS service models

## [0.27.1-beta] - 06/19/2023

### Fixes
* [#815](https://github.com/awslabs/aws-sdk-kotlin/issues/815) Fix a bug in forming S3 WriteGetObjectResponse's host path

### Miscellaneous
* Sync AWS service models.
* Update user agent header to new cross-SDK format

## [0.27.0-beta] - 06/09/2023

### Miscellaneous
* Upgrade smithy-kotlin and sync service models.
* [#824](https://github.com/awslabs/aws-sdk-kotlin/issues/824) **BREAKING:** Update closeability of various types of CredentialsProvider

## [0.26.1-beta] - 06/01/2023

### Fixes
* Fix infinite pagination in S3 `ListParts`

### Miscellaneous
* Sync AWS models and bump smithy-kotlin

## [0.26.0-beta] - 05/25/2023

### Features
* [#755](https://github.com/awslabs/smithy-kotlin/issues/755) **Breaking**: Refresh presigning APIs to simplify usage and add new capabilities. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/925) for more information.

### Miscellaneous
* Sync the latest versions of AWS service models and **smithy-kotlin**

## [0.25.0-beta] - 05/19/2023

### Features
* **Breaking**: Make HTTP engines configurable in client config during initialization and during `withCopy`. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/new?category=announcements) for more information.

### Miscellaneous
* Sync smithy-kotlin and AWS service models.

## [0.24.0-beta] - 05/12/2023

### Features
* [#806](https://github.com/awslabs/aws-sdk-kotlin/issues/806) Add support for IAM Identity Center authentication and sso-session support in shared config

### Miscellaneous
* **BREAKING**: Refactor `SsoCredentialsProvider` to take an `SsoSession` parameter.
* Sync smithy-kotlin and service models.

## [0.23.0-beta] - 05/04/2023

### Features
* [#432](https://github.com/awslabs/aws-sdk-kotlin/issues/432) Enable resolving `LogMode` from environment

### Miscellaneous
* Sync AWS models and **smithy-kotlin** to the latest versions
* Refactor environment settings and retry modes into smithy-kotlin
* Sync AWS models and bump smithy-kotlin version

## [0.22.1-beta] - 04/21/2023

### Features
* BREAKING: Add support for retrying certain transient HTTP errors. `RetryErrorType.Timeout` was renamed to `RetryErrorType.Transient`.

### Miscellaneous
* Refactor internal endpoint resolver execution to track upstream changes.
* Sync AWS models and bump smithy-kotlin version

## [0.22.0-beta] - 04/14/2023

### Miscellaneous
* Refactor identity and authentication APIs
* Upgrade smithy-kotlin and sync AWS service models.

## [0.21.5-beta] - 04/06/2023

### Fixes
* [#492](https://github.com/awslabs/aws-sdk-kotlin/issues/492) Don't use potentially stale profile when retrieving credentials via IMDS.

### Miscellaneous
* Upgrade **smithy-kotlin** to 0.16.6
* Sync AWS models to latest versions
* Upgrade smithy to pull in protocol tests for intEnum support.

## [0.21.4-beta] - 03/30/2023

### Features
* Add support for awsQuery-compatible error responses.

### Miscellaneous
* add clarifying docs for endpointUrl
* Sync latest AWS models

## [0.21.3-beta] - 03/16/2023

### Features
* [#206](https://github.com/awslabs/aws-sdk-kotlin/issues/206) Add support for loading FIPS and dual-stack endpoint settings from env, system properties, and shared config.
* [#206](https://github.com/awslabs/aws-sdk-kotlin/issues/206) Add support for loading S3 accelerate and addressing settings from shared config.

### Fixes
* [#874](https://github.com/awslabs/aws-sdk-kotlin/issues/874) Ensure all unsigned operations are accessible without credentials in CognitoIdentityProvider.

### Miscellaneous
* Update smithy-kotlin version and sync service models.

## [0.21.2-beta] - 03/09/2023

### Features
* Add sub-property support for AWS config.

### Miscellaneous
* Sync AWS models and bump smithy-kotlin version

## [0.21.1-beta] - 03/02/2023

### Miscellaneous
* Sync AWS service models and **smithy-kotlin** version

## [0.21.0-beta] - 02/24/2023

### Features
* Sync AWS service models and **smithy-kotlin** version

### Miscellaneous
* Refactor: move CachedCredentialsProvider and CredentialsProviderChain to smithy-kotlin under aws.smithy.kotlin.runtime.auth.awscredentials package

## [0.20.3-beta] - 02/16/2023

### Features
* [#839](https://github.com/awslabs/aws-sdk-kotlin/issues/839) Add `Expect: 100-continue` header to S3 PUT requests over 2MB

### Miscellaneous
* Upgrade smithy-kotlin and sync latest service models.

## [0.20.2-beta] - 02/09/2023

### Features
* Add configuration for retry policy on clients

### Fixes
* [#836](https://github.com/awslabs/aws-sdk-kotlin/issues/836) Fix bug caused by reading too few bytes when parsing header values in event streams

### Miscellaneous
* Sync AWS service models
* Refactor: track upstream module changes
* Refactor: track upstream HTTP module changes

## [0.20.1-beta] - 02/06/2023

### Features
* Sync to latest AWS service models
* [#446](https://github.com/awslabs/smithy-kotlin/issues/446) Implement flexible checksums customization

### Miscellaneous
* Update to latest **smithy-kotlin** version
* Upgrade to Kotlin 1.8.10
* Refactor: track upstream module refactoring

## [0.20.0-beta] - 01/31/2023

### Features
* add ProcessCredentialsProvider which invokes a user-specified command to retrieve credentials
* Allow config override for one or more operations with an existing service client.

### Miscellaneous
* **Breaking** Remove `Closeable` supertype from `HttpClientEngine` interface. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/818) for more information.
* Refactor the way service client configuration is generated
* Upgrade Kotlin version to 1.8.0
* Update to latest AWS service models.
* Upgrade dependencies

## [0.19.5-beta] - 01/19/2023

### Miscellaneous
* Sync AWS service models.

## [0.19.4-beta] - 01/13/2023

### Miscellaneous
* Sync AWS models

## [0.19.3-beta] - 01/12/2023
**NOTE**: Do not use. Prefer 0.19.4-beta or later.

### Features
* [#122](https://github.com/awslabs/smithy-kotlin/issues/122) Add capability to intercept SDK operations

### Miscellaneous
* Sync AWS service models

## [0.19.2-beta] - 12/22/2022

### Fixes
* Correct validation of empty segments in ARN parser

### Miscellaneous
* Upgrade smithy-kotlin and sync AWS models.

## [0.19.1-beta] - 12/15/2022

### Miscellaneous
* Sync AWS service models

## [0.19.0-beta] - 12/01/2022

### Miscellaneous
* Upgrade smithy-kotlin and sync service models and partitions.
* **BREAKING** Refactor SDK I/O types. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/768) for more information

## [0.18.0-beta] - 11/23/2022

### Features
* Add support for dual-stack endpoints in client config.
* [#399](https://github.com/awslabs/aws-sdk-kotlin/issues/399) Add support for [S3 Virtual Host Addressing](https://docs.aws.amazon.com/AmazonS3/latest/userguide/VirtualHosting.html) (enabled by default).
* [#231](https://github.com/awslabs/aws-sdk-kotlin/issues/231) Add support for [S3 Access Points](https://aws.amazon.com/s3/features/access-points/).
* Add support for [S3 Object Lambda](https://aws.amazon.com/s3/features/object-lambda/).
* [#677](https://github.com/awslabs/smithy-kotlin/issues/677) Add a new tracing framework for centralized handling of log messages and metric events and providing easy integration points for connecting to downstream tracing systems (e.g., kotlin-logging)
* **BREAKING** Add smithy-modeled endpoint resolvers for AWS services. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/761) for more information.
* Add support for [S3 PrivateLink](https://docs.aws.amazon.com/AmazonS3/latest/userguide/privatelink-interface-endpoints.html).
* Add support for [FIPS](https://aws.amazon.com/compliance/fips/) endpoints in client config.
* Add support for [S3 Transfer Acceleration](https://docs.aws.amazon.com/AmazonS3/latest/userguide/transfer-acceleration.html).
* Add support for [S3 Outposts](https://aws.amazon.com/s3/outposts/).

### Miscellaneous
* Sync AWS service models

## [0.17.12-beta] - 11/15/2022

### Fixes
* [#753](https://github.com/awslabs/aws-sdk-kotlin/issues/753) Upgrade smithy-kotlin to fix Android crash when OkHttp response body coroutine throws an exception

### Miscellaneous
* Sync AWS models to latest

## [0.17.11-beta] - 11/10/2022

### Miscellaneous
* Sync AWS service models

## [0.17.10-beta] - 11/03/2022

### Miscellaneous
* Upgrade smithy to 1.26.1.
* Sync models and bump smithy-kotlin version for release.

## [0.17.9-beta] - 10/27/2022

### Fixes
* #711 Pass client configuration's httpClientEngine to the CredentialsProvider and region to ProfileCredentialsProvider
* [#733](https://github.com/awslabs/aws-sdk-kotlin/issues/733) Fix OkHttp engine crashing on Android when coroutine is cancelled while uploading request body

## [0.17.8-beta] - 10/14/2022

### Features
* #707 Support static stability for IMDS credentials

### Fixes
* [#715](https://github.com/awslabs/aws-sdk-kotlin/issues/715) Enable intra-repo links in API ref docs

### Miscellaneous
* Sync AWS service models

## [0.17.7-beta] - 10/03/2022

### Features
* #486 Enable configurability of the retry strategy through environment variables, system properties, and AWS profiles.

### Fixes
* [#697](https://github.com/awslabs/aws-sdk-kotlin/issues/697) Correct handling of non-success responses when retrieving credentials on ECS.

### Miscellaneous
* Upgrade smithy-kotlin version.
* Update/clarify changelog and add commit instructions in the Contributing Guidelines
* Upgrade Kotlin version and dependencies in ECS credentials integration test.
* Sync AWS models and upgrade smithy-kotlin.

## [0.17.6-beta] - 09/19/2022

### Features
* [#543](https://github.com/awslabs/aws-sdk-kotlin/issues/543) Add support for event streams
* Mark event stream HTTP body as duplex stream

### Fixes
* [#694](https://github.com/awslabs/aws-sdk-kotlin/issues/694) Merge per-op custom metadata to avoid clobbering per-client metadata

### Miscellaneous
* Sync AWS service models
* Update smithy-kotlin version
* Add unbound event stream payload deserialization path
* Use explict CoroutineScope for consuming event stream flow

## [0.17.5-beta] - 08/18/2022

### Fixes
* [#55](https://github.com/awslabs/aws-crt-kotlin/issues/55) Upgrade smithy-kotlin dependency to fix Mac dlopen issue
* [#601](https://github.com/awslabs/aws-sdk-kotlin/issues/601) Remove incorrect `.` at end of license identifier header in source files.

### Documentation
* [#683](https://github.com/awslabs/aws-sdk-kotlin/issues/683) Enhance **CONTRIBUTING.md** with additional details about required PR checks and how to run them locally

### Miscellaneous
* Upgrade smithy-kotlin to latest released version, 0.12.5
* Upgrade ktlint to 0.46.1.
* Sync AWS service models
* Upgrade Smithy to 1.23.0, upgrade Smithy Gradle to 0.6.0

## [0.17.4-beta] - 08/11/2022

### Fixes
* Update event stream model test template

### Miscellaneous
* Upgrade Kotlin version to 1.7.10
* Upgrade smithy-kotlin to 0.12.4.

## [0.17.3-beta] - 08/04/2022

### Miscellaneous
* Sync AWS service models

## [0.17.2-beta] - 07/28/2022

### Miscellaneous
* Sync AWS service models.
* [#216](https://github.com/awslabs/smithy-kotlin/issues/216) Enable [Explicit API mode](https://github.com/Kotlin/KEEP/blob/master/proposals/explicit-api-mode.md)

## [0.17.1-beta] - 07/21/2022

### Features
* [#509](https://github.com/awslabs/aws-sdk-kotlin/issues/509) Implement ID prefix trimming for route53 resources.

### Miscellaneous
* Sync AWS service models.

## [0.17.0-beta] - 07/14/2022

### Fixes
* **Breaking**: Move DSL overloads on generated clients to extension methods

### Miscellaneous
* Sync AWS service models.
* **Breaking**: Upgrade **smithy-kotlin** version which will replace all instances of `Set<T>` with `List<T>` in service shapes

## [0.16.6-beta] - 07/08/2022

### Features
* [#123](https://github.com/awslabs/smithy-kotlin/issues/123) Add support for smithy Document type.

### Miscellaneous
* Update AWS models to latest versions
* Upgrade smithy-kotlin version to 0.11.2
* [#599](https://github.com/awslabs/smithy-kotlin/issues/599) Upgrade Smithy version to 1.22

## [0.16.5-beta] - 07/01/2022

### Miscellaneous
* [#622](https://github.com/awslabs/aws-sdk-kotlin/issues/622) Upgrade Kotlin to 1.7

## [0.16.4-beta] - 06/23/2022

### Fixes
* [#139](https://github.com/awslabs/smithy-kotlin/issues/139) Validate that members bound to URI paths are non-null at object construction

### Miscellaneous
* Upgrade smithy kotlin to [0.11.0](https://github.com/awslabs/smithy-kotlin/releases/tag/v0.11.0)

## [0.16.3-beta] - 06/16/2022

### Features
* Support bootstrapping services by package name (in addition to by model filename)

### Documentation
* Update the debugging guide to demonstrate how to use Log4j2 for logging

### Miscellaneous
* Sync AWS models to latest

## [0.16.2-beta] - 06/10/2022

### Fixes
* [#619](https://github.com/awslabs/aws-sdk-kotlin/issues/619), [#657](https://github.com/awslabs/smithy-kotlin/issues/657) Upgrade smithy-kotlin to pull in fixes for signing bugs.

### Documentation
* [#620](https://github.com/awslabs/aws-sdk-kotlin/issues/620) Update outdated howto docs to correctly describe client instantiation and client engine configuration

### Miscellaneous
* Sync AWS models to latest

## [0.16.1-beta] - 06/02/2022

### Features
* [#617](https://github.com/awslabs/smithy-kotlin/issues/617) Add a new non-CRT SigV4 signer and use it as the default. This removes the CRT as a hard dependency for using the SDK (although the CRT signer can still be used via explicit configuration on client creation).

### Miscellaneous
* Sync AWS models to latest

## [0.16.0] - 05/26/2022

### Features
* [#460](https://github.com/awslabs/aws-sdk-kotlin/issues/460) Enhance generic codegen to be more KMP-friendly. This is a **breaking change** which means service client artifacts will now include their platform name (e.g., `s3-jvm-<version>.jar` vs `s3-<version>.jar`). Users consuming dependencies through the Gradle Kotlin plugin will have this handled automatically for them.

### Fixes
* [#480](https://github.com/awslabs/aws-sdk-kotlin/issues/480) Upgrade smithy-kotlin to 0.10.0 which upgrades to ktor-2.x. This is considered a **breaking change** as it may reverse the issue described in #480 and break ktor-1.x users.

### Miscellaneous
* Upgrade smithy-kotlin to 0.9.2 which includes codegen updates to generate operations with all optional inputs to include a default parameter. See [smithy-kotlin#129](https://github.com/awslabs/smithy-kotlin/issues/129)
* upgrade kotlin to 1.6.21 and other deps to latest

## [0.15.2-beta] - 05/13/2022

### Features
* Implement recursion detection middleware.
* [#575](https://github.com/awslabs/aws-sdk-kotlin/issues/575) Add support for detecting custom metadata in system properties (starting with `aws.customMetadata.`) and environment variables (starting with `AWS_CUSTOM_METADATA_`)

## [0.15.1-beta] - 04/29/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Documentation
* update API reference doc styling

### Miscellaneous
* Sync latest AWS service models
* Refactor hashing functions into new subproject

## [0.15.0] - 04/29/2022

**NOTE**: Do not use. Prefer 0.15.1-beta or later.

## [0.14.4-beta] - 04/21/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes
* CreateMultipartUpload doesn't get signed correctly [#588](https://github.com/awslabs/aws-sdk-kotlin/issues/588)
* Possible memory leak in new default HTTP engine [#587](https://github.com/awslabs/aws-sdk-kotlin/issues/587)

### Miscellaneous
* sync AWS models [#590](https://github.com/awslabs/aws-sdk-kotlin/pull/590)

## [0.14.3-beta] - 04/14/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes
* resolve region only when profile credentials require it [#582](https://github.com/awslabs/aws-sdk-kotlin/pull/582)
* only set Content-Type when appropriate [#570](https://github.com/awslabs/aws-sdk-kotlin/pull/570)

### Miscellaneous
* sync AWS models [#585](https://github.com/awslabs/aws-sdk-kotlin/pull/585)

## [0.14.2-beta] - 04/07/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes

* fix timeout on large requests [#572](https://github.com/awslabs/aws-sdk-kotlin/issues/572)

## [0.14.1-beta] - 03/31/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### New features
* implement KMP XML serde and remove XmlPull dependency [#563](https://github.com/awslabs/aws-sdk-kotlin/pull/563)

### Miscellaneous
* sync AWS service models [#564](https://github.com/awslabs/aws-sdk-kotlin/pull/564)

## [0.14.0-beta] - 03/24/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* replace default HTTP client engine [#554](https://github.com/awslabs/aws-sdk-kotlin/pull/554)

### New features
* bootstrap event streams [#545](https://github.com/awslabs/aws-sdk-kotlin/pull/545)

### Fixes
* temporarily bypass httpchecksum traits until full flexible checksum support is available [#558](https://github.com/awslabs/aws-sdk-kotlin/pull/558)
* include headers in presigning requests [#556](https://github.com/awslabs/aws-sdk-kotlin/pull/556)
* backfill optional auth trait for cognito and cognito-idp [#555](https://github.com/awslabs/aws-sdk-kotlin/pull/555)

### Miscellaneous
* update AWS models to latest versions [#559](https://github.com/awslabs/aws-sdk-kotlin/pull/559)
* cleanup presign tests [#546](https://github.com/awslabs/aws-sdk-kotlin/pull/546)

## [0.13.1-beta] - 02/25/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes
* improve detection of available read bytes to avoid hang [#535](https://github.com/awslabs/aws-sdk-kotlin/pull/535)

## [0.13.0-beta] - 02/17/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* add sso credential provider; make all providers kmp compatible [#469](https://github.com/awslabs/aws-sdk-kotlin/pull/469)

### New features
* update AWS models to latest versions [#532](https://github.com/awslabs/aws-sdk-kotlin/pull/532)

### Fixes
* bump crt-kotlin to latest to fix leaked connections [#529](https://github.com/awslabs/aws-sdk-kotlin/pull/529)
* isClosedForRead implies availableForRead is zero
* fix CRT read channel buffer management

### Miscellaneous
* coroutine version bump to 1.6.0 and Duration stabilization [#514](https://github.com/awslabs/aws-sdk-kotlin/pull/514)
* dokka upgrade [#523](https://github.com/awslabs/aws-sdk-kotlin/pull/523)
* upgrade smithy to 1.17.0 [#521](https://github.com/awslabs/aws-sdk-kotlin/pull/521)

## [0.12.0-beta] - 02/04/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* enable waiters

### Fixes
* propagate crt stream errors to response body consumer [#510](https://github.com/awslabs/aws-sdk-kotlin/pull/510)

## [0.11.0-beta] - 01/20/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* Generate presigner into nested package for consistency [#502](https://github.com/awslabs/aws-sdk-kotlin/pull/502)

### New features
* update AWS models to latest versions [#505](https://github.com/awslabs/aws-sdk-kotlin/pull/505)

## [0.10.1-beta] - 01/13/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### New features
* update AWS models to latest versions [#499](https://github.com/awslabs/aws-sdk-kotlin/pull/499)
* Paginators! [smithy-kotlin#557](https://github.com/awslabs/smithy-kotlin/pull/557)

### Fixes
* enforce only once shutdown logic for crt engine connections [#497](https://github.com/awslabs/aws-sdk-kotlin/pull/497)

## [0.10.0-beta] - 01/06/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* Update codegen to generate base exception rather than UnknownServiceErrorException [#484](https://github.com/awslabs/aws-sdk-kotlin/pull/484)

### New features
* update AWS models to latest versions
* upgrade to Kotlin 1.6.10 [#474](https://github.com/awslabs/aws-sdk-kotlin/pull/474)

### Fixes
* Fix usage of unicode in bucket names of s3 presigner [#487](https://github.com/awslabs/aws-sdk-kotlin/pull/487)
* Add new services in published release [#468](https://github.com/awslabs/aws-sdk-kotlin/pull/468)

### Miscellaneous
* Add design tenets [#466](https://github.com/awslabs/aws-sdk-kotlin/pull/466)
* updated the Readme doc to include API reference guide [#471](https://github.com/awslabs/aws-sdk-kotlin/pull/471)

## [0.9.5-beta] - 12/09/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Services new in this release

* amplifyuibuilder
* appconfigdata
* backupgateway
* chimesdkmeetings
* drs
* evidently
* inspector2
* iottwinmaker
* migrationhubrefactorspaces
* migrationhubstrategy
* rbin
* resiliencehub
* rum
* workspacesweb

### Fixes
* move endpoint resolution log messages from DEBUG to TRACE [#443](https://github.com/awslabs/aws-sdk-kotlin/pull/443)
* presigner cleanup [#452](https://github.com/awslabs/aws-sdk-kotlin/pull/452)
* import signing test suite [#451](https://github.com/awslabs/aws-sdk-kotlin/pull/451)

## [0.9.4-beta] - 12/01/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

## [0.9.3-alpha] - 11/19/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes
* render default endpoint resolver for machinelearning [#424](https://github.com/awslabs/aws-sdk-kotlin/pull/424)

## [0.9.2-alpha] - 11/11/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### New features
* SDK generation and build docs [#377](https://github.com/awslabs/aws-sdk-kotlin/pull/377)

### Fixes
* disable signing for sts operations AssumeRoleWithSaml and AssumeRoleWithWebIdentity [#407](https://github.com/awslabs/aws-sdk-kotlin/pull/407)

### Miscellaneous
* Add howto to override default http client. [#412](https://github.com/awslabs/aws-sdk-kotlin/pull/412)

## [0.9.1-alpha] - 11/04/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### New features
* implement retries for imds [#404](https://github.com/awslabs/aws-sdk-kotlin/pull/404)
* enable machinelearning endpoint customization [#378](https://github.com/awslabs/aws-sdk-kotlin/pull/378)
* add glacier request body checksum [#379](https://github.com/awslabs/aws-sdk-kotlin/pull/379)

### Fixes
* restJson1 empty httpPayload body serialization [#402](https://github.com/awslabs/aws-sdk-kotlin/pull/402)

## [0.9.0-alpha] - 10/28/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes

* overhaul endpoint resolver types [#361](https://github.com/awslabs/aws-sdk-kotlin/pull/361)

### New features

* extend user agent metadata with framework, feature, and config [#372](https://github.com/awslabs/aws-sdk-kotlin/pull/372)

### Misc

* add sync models task and sync latest models [#374](https://github.com/awslabs/aws-sdk-kotlin/pull/374)

## [0.8.0-alpha] - 10/21/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* require a resolved configuration [#351](https://github.com/awslabs/aws-sdk-kotlin/pull/351)

### New features
* detect region from active AWS profile [#344](https://github.com/awslabs/aws-sdk-kotlin/pull/344)
* ec2 imds region provider [#341](https://github.com/awslabs/aws-sdk-kotlin/pull/341)
* Add STS assume role and web identity credential providers [#352](https://github.com/awslabs/aws-sdk-kotlin/pull/352)
* ECS credential provider [#353](https://github.com/awslabs/aws-sdk-kotlin/pull/353)
* ec2 credentials provider [#348](https://github.com/awslabs/aws-sdk-kotlin/pull/348)

### Fixes
* use wrapped response when deserializing modeled exceptions [#358](https://github.com/awslabs/aws-sdk-kotlin/pull/358)
* switch from ULong to Long in public presigner API for better java interop [#359](https://github.com/awslabs/aws-sdk-kotlin/pull/359)

### Misc
* Bump Kotlin and Dokka versions to latest release [#360](https://github.com/awslabs/aws-sdk-kotlin/pull/360)
* update aws models [#347](https://github.com/awslabs/aws-sdk-kotlin/pull/347)
* add docs for enabling logging in unit tests [#339](https://github.com/awslabs/aws-sdk-kotlin/pull/339)


## [0.7.0-alpha] - 10/14/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

## New features

* http engine config [#336](https://github.com/awslabs/aws-sdk-kotlin/pull/336)
* add codegen wrappers for retries [#331](https://github.com/awslabs/aws-sdk-kotlin/pull/331)

## [0.6.0-alpha] - 10/07/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

## New features

* implement basic retry support in runtime [#328](https://github.com/awslabs/aws-sdk-kotlin/pull/328)
* event stream framing support [#320](https://github.com/awslabs/aws-sdk-kotlin/pull/320)
* replace GSON based JSON serde with KMP compatible impl [#477](https://github.com/awslabs/smithy-kotlin/pull/477)
* Add IMDSv2 client in runtime [#330](https://github.com/awslabs/aws-sdk-kotlin/pull/330)


## [0.5.0-alpha] - 09/30/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes

* split auth and signing packages [#318](https://github.com/awslabs/aws-sdk-kotlin/issues/318)
    * Import paths changed to `aws.sdk.kotlin.runtime.auth.credentials` and `aws.sdk.kotlin.runtime.auth.signing`

### New features

* autofill Glacier accountId [#246](https://github.com/awslabs/aws-sdk-kotlin/issues/246)
* support JVM system property and environment variables for profiles [#297](https://github.com/awslabs/aws-sdk-kotlin/issues/297)
* expose method to sign standalone requests [#318](https://github.com/awslabs/aws-sdk-kotlin/issues/318)
* AWS configuration loader and parser [#216](https://github.com/awslabs/aws-sdk-kotlin/issues/216)

### Fixes

* utilize custom endpoint ports [#310](https://github.com/awslabs/aws-sdk-kotlin/issues/310)
* Replace junit imports with kotlin.test imports where possible [#321](https://github.com/awslabs/aws-sdk-kotlin/issues/321)
* update readme to include latest version [#319](https://github.com/awslabs/aws-sdk-kotlin/issues/319)
* sync models and endpoints [#317](https://github.com/awslabs/aws-sdk-kotlin/issues/317)
* Favor kotlin-test-juint5 over kotlin-test to resolve intermittent build failures [#316](https://github.com/awslabs/aws-sdk-kotlin/issues/316)
* kotlin 1.5.30, coroutine, kotest version bumps [#307](https://github.com/awslabs/aws-sdk-kotlin/issues/307)


## [0.4.0-alpha] - 08/26/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Services new in this release

* ec2
* location
* marketplacecommerceanalytics

### New features

* Support for presigning requests [#435](https://github.com/awslabs/smithy-kotlin/issues/435)
* Detect aws region from system properties [#202](https://github.com/awslabs/aws-sdk-kotlin/issues/202)
* EC2 Query Protocol [#230](https://github.com/awslabs/aws-sdk-kotlin/issues/230)
* Provide opt-in wire logging [#425](https://github.com/awslabs/smithy-kotlin/issues/425)
* Support profile credentials provider [#302](https://github.com/awslabs/smithy-kotlin/issues/302)

### Fixes

* s3.deleteObjects causes an exception [#125](https://github.com/awslabs/aws-sdk-kotlin/issues/125)
* Streaming request BodyStream never read [#282](https://github.com/awslabs/aws-sdk-kotlin/issues/282)
* location service references traits not in sdk classpath [#286](https://github.com/awslabs/aws-sdk-kotlin/issues/286)
* Ignore unboxed types for subset of services [#261](https://github.com/awslabs/aws-sdk-kotlin/issues/261)
* Service operations specifying no auth should not sign requests with sigv4 [#263](https://github.com/awslabs/aws-sdk-kotlin/issues/263)
* Create S3 object with Unicode name fails with signature mismatch [#200](https://github.com/awslabs/aws-sdk-kotlin/issues/200)
* Codegen errors in marketplacecommerceanalytics [#214](https://github.com/awslabs/aws-sdk-kotlin/issues/214)
* Escape model-extra files for Windows [#191](https://github.com/awslabs/aws-sdk-kotlin/issues/191)
* Support Glacier APIVersion Header [#165](https://github.com/awslabs/smithy-kotlin/issues/165)
* Support APIGateway Accept Header [#157](https://github.com/awslabs/smithy-kotlin/issues/157)
* Add support for awsQueryError trait [#375](https://github.com/awslabs/smithy-kotlin/issues/375)
* S3 HeadObject errors require customization [#152](https://github.com/awslabs/aws-sdk-kotlin/issues/152)
* S3 custom treatment of GetBucketLocation response [#194](https://github.com/awslabs/aws-sdk-kotlin/issues/194)

## [0.3.0-M2] - 06/18/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Services new in this release

* applicationcostprofiler
* apprunner
* autoscaling
* cloudformation
* cloudsearch
* cloudwatch
* docdb
* elasticache
* elasticbeanstalk
* elasticloadbalancing
* elasticloadbalancingv2
* finspace
* finspacedata
* iam
* neptune
* nimble
* rds
* redshift
* ses
* sns
* sqs
* ssmcontacts
* ssmincidents
* sts

## Changes

### New Features

* `awsQuery` protocol support (https://github.com/awslabs/smithy-kotlin/issues/127)
* detect region from environment variables (https://github.com/awslabs/smithy-kotlin/issues/356)
* custom S3 error metadata support (https://github.com/awslabs/smithy-kotlin/issues/323)
* environment credentials provider (https://github.com/awslabs/smithy-kotlin/issues/301)
* bind default HTTP client engine to CRT (https://github.com/awslabs/smithy-kotlin/issues/199)
* operation DSL overloads (https://github.com/awslabs/smithy-kotlin/issues/184)
* Kinesis read timeouts (https://github.com/awslabs/smithy-kotlin/issues/162)
* XML EOL encoding support (https://github.com/awslabs/smithy-kotlin/issues/142)

### Fixes

* path literal not escaped correctly (https://github.com/awslabs/smithy-kotlin/issues/395)
* idempotency tokens are not detected on resources (https://github.com/awslabs/smithy-kotlin/issues/376)
* httpPayload bound members need dedicated serde (https://github.com/awslabs/smithy-kotlin/issues/353)
* Route53 error messages (and maybe other restXml messages) are not deserialized and present in stacktrace
  (https://github.com/awslabs/smithy-kotlin/issues/352)
* fix url-encoding behavior of httpLabel and httpQuery members (https://github.com/awslabs/smithy-kotlin/issues/328)
* runtime error when using Kotlin 1.5.0 (https://github.com/awslabs/smithy-kotlin/issues/319)
* SES fails to build due to invalid docs (https://github.com/awslabs/aws-sdk-kotlin/issues/153)
* exception is thrown for SQS delete message (https://github.com/awslabs/aws-sdk-kotlin/issues/147)
* SNS getTopicAttributes throws an exception (https://github.com/awslabs/aws-sdk-kotlin/issues/142)
* elasticbeanstalk model generates invalid enum (https://github.com/awslabs/smithy-kotlin/issues/403)

### Other

* Kotlin 1.5.0 support
* design docs added to [docs/design](docs/design) directory

## [0.2.0-M1] - 05/10/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Known Issues

* The Kotlin SDK is not compatible with Kotlin 1.5.0.  Please use Kotlin 1.4.x.

### Services new in this release

* accessanalyzer
* acm
* acmpca
* alexaforbusiness
* amp
* amplify
* amplifybackend
* appconfig
* appflow
* appintegrations
* applicationautoscaling
* applicationdiscoveryservice
* applicationinsights
* appmesh
* appstream
* appsync
* athena
* auditmanager
* autoscalingplans
* backup
* batch
* braket
* budgets
* build
* chime
* cloud9
* clouddirectory
* cloudfront
* cloudhsm
* cloudhsmv2
* cloudsearchdomain
* cloudtrail
* cloudwatchevents
* cloudwatchlogs
* codeartifact
* codebuild
* codecommit
* codedeploy
* codeguruprofiler
* codegurureviewer
* codepipeline
* codestar
* codestarconnections
* codestarnotifications
* cognitoidentity
* cognitosync
* comprehend
* comprehendmedical
* computeoptimizer
* configservice
* connect
* connectcontactlens
* connectparticipant
* costandusagereportservice
* costexplorer
* customerprofiles
* databasemigrationservice
* databrew
* dataexchange
* datapipeline
* datasync
* dax
* detective
* devicefarm
* devopsguru
* directconnect
* directoryservice
* dlm
* dynamodbstreams
* ebs
* ec2instanceconnect
* ecr
* ecrpublic
* ecs
* efs
* eks
* elasticinference
* elasticsearchservice
* elastictranscoder
* emr
* emrcontainers
* eventbridge
* fis
* fms
* forecast
* forecastquery
* frauddetector
* fsx
* glacier
* globalaccelerator
* greengrass
* greengrassv2
* groundstation
* guardduty
* health
* healthlake
* honeycode
* identitystore
* imagebuilder
* inspector
* ivs
* kafka
* kendra
* kinesis
* kinesisanalytics
* kinesisanalyticsv2
* kinesisvideo
* kinesisvideoarchivedmedia
* kinesisvideomedia
* kinesisvideosignaling
* lakeformation
* lexmodelbuildingservice
* lexmodelsv2
* lexruntimeservice
* lexruntimev2
* licensemanager
* lookoutequipment
* lookoutmetrics
* lookoutvision
* machinelearning
* macie
* macie2
* managedblockchain
* marketplacecatalog
* marketplaceentitlementservice
* marketplacemetering
* mediaconnect
* mediapackage
* mediapackagevod
* mediastore
* mediastoredata
* mediatailor
* mgn
* migrationhub
* migrationhubconfig
* mobile
* mq
* mturk
* mwaa
* networkfirewall
* networkmanager
* opsworks
* opsworkscm
* organizations
* outposts
* personalize
* personalizeevents
* personalizeruntime
* pi
* pinpointemail
* pinpointsmsvoice
* pricing
* qldb
* qldbsession
* quicksight
* ram
* rdsdata
* redshiftdata
* rekognition
* repocache
* resourcegroups
* resourcegroupstaggingapi
* robomaker
* route53
* route53domains
* route53resolver
* s3
  * NOTE: S3 is a complicated service, this initial release **DOES NOT** have complete support for all S3 features.
* s3control
* s3outposts
* sagemaker
* sagemakera2iruntime
* sagemakeredge
* sagemakerfeaturestoreruntime
* sagemakerruntime
* savingsplans
* schemas
* serverlessapplicationrepository
* servicecatalog
* servicecatalogappregistry
* servicediscovery
* servicequotas
* sesv2
* sfn
* shield
* signer
* sms
* snowball
* sso
* ssoadmin
* ssooidc
* storagegateway
* support
* swf
* synthetics
* textract
* timestreamquery
* timestreamwrite
* transcribe
* transfer
* waf
* wafregional
* wafv2
* wellarchitected
* workdocs
* worklink
* workmail
* workmailmessageflow
* workspaces
* xray

## Changes

### New Features

* `restXml` protocol support
* add conversions to/from `java.time.Instant` and SDK `Instant` (https://github.com/awslabs/smithy-kotlin/issues/271)
* implement missing IO runtime primitives (https://github.com/awslabs/smithy-kotlin/issues/264)
* API documentation (https://github.com/awslabs/aws-sdk-kotlin/issues/119)

### Fixes

* generate per/service base exception types (https://github.com/awslabs/smithy-kotlin/issues/233)
* use sdkId if available for service client generation (https://github.com/awslabs/smithy-kotlin/issues/276)
* explicitly set jvm target compatibility (https://github.com/awslabs/aws-sdk-kotlin/issues/103)
* http error registration (https://github.com/awslabs/aws-sdk-kotlin/issues/118)

### Other

* generate per/service base exception types (https://github.com/awslabs/smithy-kotlin/issues/270)

## [0.1.0-M0] - 03/19/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

This is the initial beta release of AWS SDK Kotlin. It represents an early look at the overall API surface.


See the [Getting Started Guide](docs/GettingStarted.md) for how to work with beta releases and examples.


### Services in this release

* DynamoDB
* Polly
* Translate
* Cognito Identity Provider
* Secrets Manager
  * NOTE: Default idempotency token provider will not currently work, you'll need to override the config to create or update secrets until [#180](https://github.com/awslabs/smithy-kotlin/issues/180) is implemented
* KMS
* Lambda

NOTES:
* We currently can (theoretically) support any JSON based AWS protocol. If there is a service you would like to see added in a future release (before developer preview) please reach out and let us know.
* No customizations are currently implemented, some SDK's may not behave 100% correctly without such support.
* Retries, waiters, paginators, and other features are not yet implemented

### Features
* Coroutine API
* DSL Builders
* Default (environment or config) or static credential providers only. Additional providers will be added in later releases.
* JVM only support (multiplatform support is on the roadmap)
