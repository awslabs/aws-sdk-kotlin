# Endpoint Resolution Design

* **Type**: Design
* **Author(s)**: Aaron Todd

# Abstract

This document covers the design of how endpoints are resolved in the Kotlin SDK

# Design

## Requirements

Smithy models do not specify an endpoint to make operational requests to. This information must be specified externally
by other means or configuration. AWS services have additional requirements that increase this complexity.


1. Endpoints must support manual configuration by end users. 
   This allows overriding default endpoint resolution and discovery and enables integrations with things such as 
   [DynamoDB Local](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html).

2. Endpoints must support being customized on a per-operation basis by the [endpoint trait](https://awslabs.github.io/smithy/1.0/spec/core/endpoint-traits.html#endpoint-trait).
    
3. Endpoints must support being customized by [endpoint discovery](https://awslabs.github.io/smithy/1.0/spec/aws/aws-core.html#client-endpoint-discovery). 
   A request, customized by a predefined set of fields from the input operation is dispatched to a specific URI. 
   That operation returns the endpoint that should be used. Endpoints must be cached by a cache key containing:
   ```
   (access_key_id, operation, [endpoint identifiers])
   ```

See [caching](https://awslabs.github.io/smithy/1.0/spec/aws/aws-core.html#caching)

NOTE: The design of endpoint discovery will be addressed separately from this document at a later date.


## Design


The AWS SDK Kotlin runtime will contain definitions of a generic endpoint type that describes the information needed to route a request:

```kt
package aws.sdk.kotlin.runtime.endpoint

/**
 * Represents the endpoint a service client should make API operation calls to.
 *
 * The SDK will automatically resolve endpoints per API client using an internal resolver.
 */
data class Endpoint(
    /**
     * The base URL endpoint clients will use to make API calls to e.g. "{service-id}.{region}.amazonaws.com"
     */
    val hostname: String,

    /**
     * The protocol to use when making a connection e.g. "HTTPS"
     */
    val protocol: String,

    /**
     * The port to connect to when making requests to this endpoint. When not specified the default port dictated
     * by the protocol will be used.
     */
    val port: Int? = null,

    /**
     * Flag indicating that the hostname can be modified by the SDK client.
     *
     * If the hostname is mutable the SDK clients may modify any part of the hostname based
     * on the requirements of the API (e.g. adding or removing content in the hostname).
     *
     * As an example Amazon S3 Client prefixing "bucketname" to the hostname or changing th hostname
     * service name component from "s3" to "s3-accespoint.dualstack." requires mutable hostnames.
     *
     * Care should be taken when setting this flag and providing a custom endpoint. If the hostname
     * is expected to be mutable and the client cannot modify the endpoint correctly, the operation
     * will likely fail.
     */
    val isHostnameImmutable: Boolean = false,

    /**
     * The service name that should be used for signing requests to this endpoint. This overrides the default
     * signing name used by an SDK client.
     */
    val signingName: String? = null,

    /**
     * The region that should be used for signing requests to this endpoint. This overrides the default
     * signing region used by an SDK client.
     */
    val signingRegion: String? = null,
)

```




```kt
/**
 * Resolves endpoints for a given service and region
 */
interface EndpointResolver {

    /**
     * Resolve the [Endpoint] for the given service and region
     */
    suspend fun resolve(service: String, region: String): Endpoint
}
```



### Default behavior

The default behavior for resolving endpoints will be to use service specific resolvers that are generated using the
metadata available in `endpoints.json`. 

Each service will get a `DefaultResolver` generated as part of their package. When a resolver is not configured on
the service client the default will be used.

The default resolver works off of the partition definitions in `endpoints.json`. During codegen the partitions and 
endpoints that apply to the service being generated will look like:


```kt

// file: aws.sdk.kotlin.{service}.internal

import aws.sdk.kotlin.runtime.endpoint.internal.*

internal class DefaultEndpointResolver : EndpointResolver {
    override suspend fun resolve(service: String, region: String): Endpoint {
        return resolveEndpoint(servicePartitions, region) ?: throw AwsClientException("cannot resolve endpoint for ${service} in the ${region} region")
    }
}

// generated from `endpoints.json`
private val servicePartitions = listOf(
    Partition(
        id = "aws",
        regionRegex = Regex("^(us|eu|ap|sa|ca|me|af)\\-\\w+\\-\\d+$"),
        isRegionalized = true,
        defaults = EndpointMeta(
            hostname = "s3.{region}.amazonaws.com",
            protocols = listOf("https", "http"),
            signatureVersions = listOf("s3v4")
        ),
        endpoints = mapOf(
            "af-south-1" to EndpointMeta(),
            "ap-northeast-1" to EndpointMeta(
                hostname = "s3.ap-northeast-1.amazonaws.com",
                signatureVersions = listOf("s3", "s3v4")
            ),
            "us-west-1" to EndpointMeta(
                hostname = "s3.us-west-1.amazonaws.com",
                signatureVersions = listOf("s3", "s3v4")
            ),

            // ...
        )
    )
)
```


The types used in generation that map to the schema of `endpoints.json` will live as part of the runtime such that
resolving an endpoint from partitions is mostly boilerplate from codegen. See the Appendix.


This follows closely to what the V2 Go SDK does.

* [Example generated partitions](https://github.com/aws/aws-sdk-go-v2/blob/main/service/s3/internal/endpoints/endpoints.go)
* [Partition definition](https://github.com/aws/aws-sdk-go-v2/blob/main/internal/endpoints/endpoints.go)
* [Endpoint definition](https://github.com/aws/aws-sdk-go-v2/blob/main/aws/endpoints.go#L13)

### Per/operation overrides

Operations are generated and executed with an `ExecutionContext` which is a typesafe property bag of metadata for 
round tripping a request.


```kt
suspend fun operation(input: OperationInput): OperationOutput {
    val execCtx = SdkHttpOperation.build {
        serializer = OperationSerializer(input)
        deserializer = OperationDeserializer()
        expectedHttpStatus = 201
        service = serviceName
        operationName = "XyzOperation"
    }

    return client.roundTrip(execCtx)

```


This allows various middleware interceptors to pull out metadata they need to execute a request 
(e.g. serialization, deserialization, signing, etc).

This execution context provides a place to insert per/operation metadata that endpoint resolution middleware 
can make use of.

e.g.


```kt
execCtx[SdkClientOption.HostPrefix] = "${input.foo}.bar-"
```


Each operation generated will look for the `endpoint trait` and add to the operations execution context a host
prefix if required.


### Manual configuration

Services will be generated with a configuration option to specify a resolver type responsible for producing the 
endpoint a request should be made to:

This allows users to supply their own endpoint resolution strategy which satisfies requirement (1).

e.g.

```kt

val client = Dynamodb {
    endpointResolver = StaticEndpointResolver("https://localhost:8000")
}

```



## Appendix

### Internal types that map to entries in `endpoints.json`

(Draft) definitions of types that codegen can make use of to generate the partition information from `endpoints.json` 
for default endpoint resolution is given below.

These types would all be considered internal to the SDK and marked with the appropriate annotation such that customers 
can’t accidentally use them.

```kt
package aws.sdk.kotlin.runtime.endpoint.internal


/**
 * A custom signing constraint for an endpoint
 *
 * @property region A custom sigv4 signing name
 * @property service A custom sigv4 service name to use when signing a request
 */
data class CredentialScope(val region: String, val service: String)

/**
 * Service endpoint metadata
 */
data class EndpointMeta(
    /**
     * A URI **template** used to resolve the hostname of the endpoint.
     * Templates are of the form `{name}`. e.g. `{service}.{region}.amazonaws.com`
     *
     * Variables that may be substituted:
     * - `service` - the service name
     * - `region` - the region name
     * - `dnsSuffix` - the dns suffix of the partition
     */
    val hostname: String? = null,

    /**
     * A list of supported protocols for the endpoint
     */
    val protocols: List<String>? = null,

    /**
     * A custom signing constraint for the endpoint
     */
    val credentialScope: CredentialScope? = null,

    /**
     * A list of allowable signature versions for the endpoint (e.g. "v4", "v2", "v3", "s3v3", etc)
     */
    val signatureVersions: List<String>? = null
)

```




```kt

/**
 *
 */
data class Partition(
    /**
     * The partition name/id e.g. "aws"
     */
    val id: String,

    /**
     * The regular expression that specified the pattern that region names in the endpoint adhere to
     */
    val regionRegex: Regex,

    /**
     * Endpoint that works across all regions or if [isRegionalized] is false
     */
    val partitionEndpoint: String,

    /**
     * Flag indicating whether or not the service is regionalized in the partition. Some services have only a single,
     * partition-global endpoint (e.g. CloudFront).
     */
    val isRegionalized: Boolean,

    /**
     * Default endpoint values for the partition. Some or all of the defaults specified may be superseded
     * by an entry in [endpoints].
     */
    val defaults: EndpointMeta,

    /**
     * Map of endpoint names to their definitions
     */
    val endpoints: Map<String, EndpointMeta>
)

```


Re-usable functions/types will be implemented that codegen delegates to. The generated `DefaultResolver` will be mostly
boilerplate over the internal functions allowing for the runtime to be re-used and unit tested.


```kt

// internal functions to be used by services to construct their generated resolver
fun resolveEndpoint(partitions: List<Partition>, region: String) Endpoint? {
    TODO("use partitions to resolve the endpoint")
}
```

NOTE: the algorithm for resolving an endpoint is described in AWSRegionsAndEndpoints


# Revision history

* 6/3/2021 - Initial upload
* 2/3/2021 - Created
