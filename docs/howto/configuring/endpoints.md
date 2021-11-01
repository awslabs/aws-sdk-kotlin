# Configuring Client Endpoints

The AWS SDK for Kotlin provides the ability to provide a custom endpoint to be used for a service. In most cases
you can just use the default `EndpointResolver` provided with each service client. There are some reasons to provide 
a custom endpoint though such as working with a pre-release version of a service or access to specific service 
features not yet modeled in the SDK (e.g. S3 has dual-stack and FIPS endpoints).


An [Endpoint Resolver](https://github.com/awslabs/aws-sdk-kotlin/blob/main/aws-runtime/aws-endpoint/common/src/aws/sdk/kotlin/runtime/endpoint/AwsEndpointResolver.kt#L11)
can be configured to provide custom endpoint resolution logic for service clients. Every
service client config is generated with an endpoint resolver that can be overridden. The endpoint resolver is given the
service and region as a string, allowing for the resolver to dynamically drive its behavior. Each service client 
package has an exported `ServiceId` constant that can be used to determine which service is invoking your endpoint 
resolver.

## Examples

The following code snippet shows how a service endpoint resolver can be overridden for S3:

```kotlin
val sharedConfig = AwsClientConfig.fromEnvironment()
val client = S3Client(sharedConfig) {
    endpointResolver = AwsEndpointResolver { service, region ->
       AwsEndpoint("https://mybucket.s3.us-west-2.amazonaws.com", CredentialScope(region = "us-west-2")) 
    }
}
```