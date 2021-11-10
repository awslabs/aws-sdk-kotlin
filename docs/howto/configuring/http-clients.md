# Configuring HTTP Clients

By default the AWS SDK for Kotlin uses an AWS HTTP client known as the AWS Common Runtime (CRT) HTTP client. This 
client was written by AWS to ensure the best experience with AWS services. However customers may choose to override 
the default HTTP client by specifying an [HttpClientEngine](https://github.com/awslabs/smithy-kotlin/blob/main/runtime/protocol/http/common/src/aws/smithy/kotlin/runtime/http/engine/HttpClientEngine.kt) 
implementation and referencing that implementation in the service client configuration at the time of client construction.

The SDK provides an additional client, `KtorEngine` via the `aws.smithy.kotlin:http-client-engine-ktor` dependency.  

Customers may also provide their own HTTP client by implementing the [HttpClientEngine](https://github.com/awslabs/smithy-kotlin/blob/main/runtime/protocol/http/common/src/aws/smithy/kotlin/runtime/http/engine/HttpClientEngine.kt)
and passing an instance of their implementation to the service client configuration.

## Example

The following code snippet demonstrates constructing an S3 client using the [Ktor OkHttp HTTP client](https://ktor.io/docs/http-client-engines.html#okhttp):

`build.gradle.kts`:
```kotlin
dependencies {
    implementation("aws.smithy.kotlin:http-client-engine-ktor:<version>")
}
```

Application code:
```kotlin
val sharedConfig = AwsClientConfig.fromEnvironment()
val client = S3Client(sharedConfig) {
    httpClientEngine = KtorEngine()
}
```