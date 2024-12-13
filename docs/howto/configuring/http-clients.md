# Configuring HTTP Clients

By default, the AWS SDK for Kotlin uses an HTTP client from OkHttp. Customers may choose to override the default HTTP
client by specifying an existing
[HttpClientEngine](https://github.com/smithy-lang/smithy-kotlin/blob/main/runtime/protocol/http/common/src/aws/smithy/kotlin/runtime/http/engine/HttpClientEngine.kt)
implementation or implementing their own and referencing that implementation in the service client configuration at the
time of client construction.

The SDK provides an additional client called `CrtHttpEngine` via the `aws.smithy.kotlin:http-client-engine-crt`
dependency. This implementation wraps the [AWS CRT](https://docs.aws.amazon.com/sdkref/latest/guide/common-runtime.html)
which provides high-performance, cross-platform implementations of common SDK features.

[![Maven][maven-badge]][maven-url]

[maven-badge]: https://img.shields.io/maven-central/v/aws.smithy.kotlin/http-client-engine-crt.svg?label=Maven
[maven-url]: https://search.maven.org/search?q=g:aws.smithy.kotlin+a:http-client-engine-crt

## Example

The following code snippet demonstrates constructing an S3 client using the CRT HTTP client:

`build.gradle.kts`:
```kotlin
dependencies {
    implementation("aws.smithy.kotlin:http-client-engine-crt:<version>")
}
```

Application code:
```kotlin
val client = S3Client.fromEnvironment {
    httpClientEngine = CrtHttpEngine()
}
```
