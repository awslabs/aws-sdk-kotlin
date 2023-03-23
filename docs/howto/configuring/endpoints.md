# Configuring Client Endpoints

AWS services are deployed across a matrix of hostname configurations. One of the first steps in making a request to an
AWS service is determining where to route that request. This process is known in the SDK as endpoint resolution.

The AWS SDK for Kotlin exposes the ability to modify this behavior within client config. In most cases, the default
configuration will suffice. However, there exist several reasons for which you may wish to modify this behavior, such as:
* making requests against a pre-release version or local deployment of a service
* access to specific service features not yet modeled in the SDK

**Disclaimer: Endpoint resolution is an advanced SDK topic. By changing these settings you risk breaking your code. The
default settings should be applicable to most users in production environments.**

## Customization

There are two primary means through which a user can control the behavior of endpoint resolution within the SDK, both of which
are exposed as config settings on each service client:

1. `endpointUrl: Url`
1. `endpointProvider: EndpointProvider`

## `endpointUrl`

Users can set a value for `endpointUrl` to indicate a "base" hostname for the instance of your service. The value set
here is not final-- it is ultimately passed as a parameter to the client's `EndpointProvider` when final resolution
occurs. The provider implementation then has the opportunity to inspect and potentially modify that value to determine
the final endpoint.

For example, if you perform an S3 `GetObject` request against a given bucket with a client where you've specified
an `endpointUrl`, the default provider implementation will inject the bucket into the hostname if it is virtual-host
compatible (assuming you haven't disabled virtual-hosting in client config).

In practice, this will most likely be used to point your client at a development or preview instance of a service.

## `EndpointProvider`

EndpointProvider is the definitive mechanism through which endpoint resolution occurs.

```kotlin
public fun interface EndpointProvider<T> {
    public suspend fun resolveEndpoint(params: T): Endpoint
}
```

The provider's `resolveEndpoint` method is invoked as part of the workflow for every request you make in the SDK. The
`Endpoint` value returned by the provider is used **as-is** when making the request.

### `EndpointProvider` parameters

Each service takes a specific set of inputs which are passed to its resolution function, defined in each service client
package as `EndpointParameters`.

Every service includes the following base parameters, which are used to facilitate general endpoint resolution within
AWS:

| name           | type      | description                                                |
|----------------|-----------|------------------------------------------------------------|
| `region`       | `String`  | The client's AWS region                                    |
| `endpoint`     | `String`  | A string representation of the value set for `endpointUrl` |
| `useFips`      | `Boolean` | Whether FIPS endpoints are enabled in client config        |
| `useDualStack` | `Boolean` | Whether dual-stack endpoints are enabled in client config  |

Services can specify additional parameters required for resolution. For example, S3's `EndpointParameters` include the
bucket name, as well as several S3-specific feature settings such as whether virtual host addressing.

If you are implementing your own provider, you should never need to construct your own instance of `EndpointParameters`.
The SDK will source the values per-request and pass them to your implementation of `resolveEndpoint`.

## `endpointUrl` vs. `EndpointProvider`

It is important to understand that the following two statements do **NOT** produce clients with equivalent endpoint
resolution behavior:

```kotlin
// using endpointUrl
S3Client.fromEnvironment { endpointUrl = Url.parse("https://endpoint.example") }

// using endpointProvider
S3Client.fromEnvironment {
    endpointProvider = object : EndpointProvider {
        override suspend fun resolveEndpoint(params: EndpointParameters): Endpoint = Endpoint("https://endpoint.example")
    }
}
```

In the former (using `endpointUrl`) statement, you are specifying a base URL to be passed to the (default) provider,
which may be modified as part of endpoint resolution.

In the latter (using `endpointProvider`), you are specifying in absolute terms that requests should be made against the
given example endpoint.

**While these two settings are not mutually exclusive, in general, you can expect to only need to modify one of them
depending on your use case. As a general SDK user, you will most often be making endpoint customizations
through `endpointUrl`.**

## A note about S3

S3 is a complex service with many of its features modeled through endpoint-related customizations, such as bucket
virtual hosting, where the bucket name is inserted into the hostname for requests.

Because of this, it is generally not recommended to replace the `EndpointProvider` implementation in S3. If you need to
extend its resolution behavior (such as sending requests to a local development stack with additional endpoint
considerations), you will most likely need to wrap the default implementation. An example of this is shown below.

## Examples

### `endpointUrl`

The following code snippet shows how the general service endpoint can be overridden for S3:

```kotlin
val client = S3Client.fromEnvironment {
    endpointUrl = Url.parse("https://custom-s3-endpoint.local")
    // endpointProvider is left as the default
}
```

### `EndpointProvider`

The following code snippet shows how one might wrap S3's default provider implementation:

```kotlin
import aws.sdk.kotlin.services.s3.endpoints.DefaultEndpointProvider as DefaultEndpointProvider
import aws.sdk.kotlin.services.s3.endpoints.EndpointParams as S3EndpointParams
import aws.sdk.kotlin.services.s3.endpoints.EndpointProvider as S3EndpointProvider
import aws.smithy.kotlin.runtime.client.endpoints.Endpoint

public class CustomS3EndpointProvider : S3EndpointProvider {
    override suspend fun resolveEndpoint(params: S3EndpointParams) =
        if (/* input params indicate we must route another endpoint for whatever reason */) {
            Endpoint(/* ... */)
        } else {
            DefaultS3EndpointProvider().resolveEndpoint(params)
        }
}
```

As demonstrated above, it is recommended to fall back to the default implementation in your own provider.

### `endpointUrl` + `endpointProvider`

The following example program demonstrates the interaction between the `endpointUrl` and `endpointProvider` settings.
**This is an advanced use case:**

```kotlin
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.endpoints.DefaultEndpointProvider as DefaultEndpointProvider
import aws.sdk.kotlin.services.s3.endpoints.EndpointParams as S3EndpointParams
import aws.sdk.kotlin.services.s3.endpoints.EndpointProvider as S3EndpointProvider
import aws.smithy.kotlin.runtime.client.endpoints.Endpoint

fun main() = runBlocking {
    S3Client.fromEnvironment {
        endpointUrl = Url.parse("https://example.endpoint")
        endpointProvider = CustomS3EndpointProvider()
    }.use { s3 ->
        // ...
    }
}

class CustomS3EndpointProvider : S3EndpointProvider {
    override suspend fun resolveEndpoint(params: S3EndpointParams) {
        println(params.endpoint) // with the above client, the value set for endpointUrl is available here
        // ...
    }
}
```