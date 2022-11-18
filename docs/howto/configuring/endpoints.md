# Configuring Client Endpoints

The AWS SDK for Kotlin exposes the ability to provide a custom endpoint to be used for a service. In most cases
you can just use the default `EndpointProvider` provided with each service client. There are some reasons to provide 
a custom endpoint though such as working with a pre-release version of a service or access to specific service 
features not yet modeled in the SDK.

An [Endpoint Provider](https://github.com/awslabs/smithy-kotlin/blob/main/runtime/protocol/http/common/src/aws/smithy/kotlin/runtime/http/endpoints/EndpointProvider.kt)
can be configured to provide custom endpoint resolution logic for service clients. Every service client config is
generated with an endpoint provider that can be overridden, where the template argument is typealiased to a set of parameters
unique to each service. Each service client package has an exported `ServiceId` constant that can be used to determine
which service is invoking your endpoint resolver.

## Examples

The following code snippet shows how a service endpoint provider can be overridden for S3:

```kotlin
import aws.sdk.kotlin.services.s3.endpoints.EndpointProvider

val client = S3Client.fromEnvironment {
    endpointProvider = EndpointProvider { // this example doesn't use the passed EndpointParameters
        Endpoint("https://mybucket.s3.us-west-2.amazonaws.com")
    }
}
```
