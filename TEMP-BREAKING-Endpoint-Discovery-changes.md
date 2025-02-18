# BREAKING: Overhaul of Endpoint Discovery features

An upcoming release of the **AWS SDK for Kotlin** will introduce improvements and fixes for
[Endpoint Discovery](https://docs.aws.amazon.com/timestream/latest/developerguide/Using-API.endpoint-discovery.how-it-works.html),
which is required when using [Amazon Timestream](https://aws.amazon.com/timestream/) and optional when using
[Amazon DynamoDB](https://aws.amazon.com/dynamodb/). If you do not use either of these services, you are unaffected by
these changes.

## Release target

This overhaul is targeted to be included in the **v1.5** release of the SDK. See
[the **v1.5** announcment](TEMP-V1Dot5-Announcment.md) for more details and other important changes.

## What's changing

The goal of this overhaul is to make Endpoint Discovery easier to configure, customize, and disable. To accomplish this,
Several aspects of Endpoint Discovery are changing:

* The service-specific endpoint discoverer classes are changing to interfaces to allow fully custom discoverer
  implementations
* Usages of the `ReadThroughCache` class are being relaxed to use the new `ExpiringKeyedCache` interface to allow fully
  custom cache implementations
  * The current implementation of `ReadThroughCache` is now provided in the new `PeriodicSweepCache` class, which
    implements the new `ExpiringKeyedCache` interface
* Endpoint Discovery settings will now be resolved from
  [environmental configuration](https://docs.aws.amazon.com/sdkref/latest/guide/feature-endpoint-discovery.html) sources
  used by other AWS SDKs, including the following sources:
  * The `aws.endpointDiscoveryEnabled` system property (JVM only)
  * The `AWS_ENABLE_ENDPOINT_DISCOVERY` environment variable
  * The `endpoint_discovery_enabled` [profile key](https://docs.aws.amazon.com/sdkref/latest/guide/file-format.html)

## How to migrate

Migrating existing code using Endpoint Discovery may require no modifications if you do not customize discovery or use
custom endpoints. Your code may require modifications in the following scenarios:

### Specifying a custom endpoint discoverer

Previously, the Timestream and DynamoDB service clients provided an `endpointDiscoverer` configuration parameter. The
type of this parameter was specific to the client. For instance, the Timestream Query client's `endpointDiscoverer` was
of type `TimestreamQueryEndpointDiscoverer`. Unfortunately, this type was a non-`open` `class` meaning there was no easy
way to provide a custom implementation.

After this update, the client-specific parameter type is now an `interface` which may be fully implemented by users. The
default implementation (e.g., `DefaultTimestreamQueryEndpointDiscoverer`) is recommended in most scenarios. If you wish
to provide a custom implementation, you may do so by setting the `endpointDiscoverer` parameter:

```kotlin
val timestreamQuery = TimestreamQueryClient.fromEnvironment {
    endpointDiscoverer = MyCustomTimestreamQueryEndpointDiscoverer()
}
```

See the client-specific interfaces (e.g., `TimestreamQueryEndpointDiscoverer`) for more details about implementation.

### Specifying custom caching behavior

Previously, the default implementations of endpoint discoverers used an internal cache to avoid re-discovering endpoints
for every new operation invocation, provided by the `ReadThroughCache` class. This meant there was no easy way to
customize or replace caching behavior.

After this update, the new `ExpiringKeyedCache` interface defines the semantics and requirements for caching while the
new `PeriodicSweepCache` class provides the old `ReadThroughCache` implementation. The default caching behavior included
in the default endpoint discoverers (e.g., `DefaultTimestreamQueryEndpointDiscoverer`) is recommended in most scenarios.
If you wish to use custom caching behavior, you may do so by passing an `ExpiringKeyedCache` implementation to the
default discoverer constructor:

```kotlin
val myCache = PeriodicSweepCache<DiscoveryParams, Host>(minimumSweepPeriod = 15.minutes)
val timestreamQuery = TimestreamQueryClient.fromEnvironment {
    endpointDiscoverer = DefaultTimestreamQueryEndpointDiscoverer(cache = myCache)
}
```

### Customizing Endpoint Discovery with environmental configuration

Previously, the settings for Endpoint Discovery were controlled through explicitly-set parameters in client
configuration because the **AWS SDK for Kotlin** did not support the
[standard environmental configuration](https://docs.aws.amazon.com/sdkref/latest/guide/feature-endpoint-discovery.html)
sources used by other AWS SDKs. This made it difficult to universally configure Endpoint Discovery at a host level,
file level, or in a multi-tenant environment.

After this update, Endpoint Discovery settings will be resolved from the following sources in descending priority:
1. Any explicit configuration provided in the client configuration parameter `endpointDiscoverer` (including
   [via `withConfig`](https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/override-client-config.html))
2. The `aws.endpointDiscoveryEnabled` system property (Kotlin/JVM only)
3. The `AWS_ENABLE_ENDPOINT_DISCOVERY ` environment variable
4. The `endpoint_discovery_enabled` profile key

A source will only be used if the higher-priority source(s) above it are not configured. For instance, the
`aws.endpointDiscoveryEnabled` system property will be ignored if `endpointDiscoverer` is explicitly configured.

## Additional info

For more information about Endpoint Discovery, see the following resources:

* [AWS SDKs and Tools Reference Guide](https://docs.aws.amazon.com/sdkref/latest/guide/feature-endpoint-discovery.html)
* [How the endpoint discovery pattern works](https://docs.aws.amazon.com/timestream/latest/developerguide/Using-API.endpoint-discovery.how-it-works.html)
* [Implementing the endpoint discovery pattern](https://docs.aws.amazon.com/timestream/latest/developerguide/Using-API.endpoint-discovery.describe-endpoints.implementation.html)
* Amazon DynamoDB's [`DescribeEndpoints` operation](https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_DescribeEndpoints.html)
* Timestream Query's [`DescribeEndpoints` operation](https://docs.aws.amazon.com/timestream/latest/developerguide/API_query_DescribeEndpoints.html)
* Timestream Write's [`DescribeEndpoints` operation](https://docs.aws.amazon.com/timestream/latest/developerguide/API_DescribeEndpoints.html)

## Feedback

If you have any questions concerning this change, please feel free to engage with us in this discussion. If you
encounter a bug with this change, please
[file an issue](https://github.com/awslabs/aws-sdk-kotlin/issues/new?template=bug_report.yml).
