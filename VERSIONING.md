# Versioning Policy

The AWS SDK for Kotlin uses a versioning scheme which follows the format: `MAJOR.MINOR.PATCH[-QUALIFIER]`. Revisions to different version components communicate the risk associated with changes when upgrading to newer versions of the SDK:

* `MAJOR` - Huge changes, expect API incompatibilities and other breaking changes.
* `MINOR` - Medium risk changes. Upgrading SHOULD usually just work but check the release notes. Example changes might include incrementing the Kotlin version, deprecating APIs, or significant changes to core runtime components. Changes to `MINOR` version MAY contain backwards incompatible changes under certain scenarios.
* `PATCH` - Zero to low risk changes. New features and bug fixes that should be safe to consume without much worry.
* `QUALIFIER` - (Optional) Additional release version qualifier (e.g. `alpha`, `beta`, `rc-1`). Most releases are not expected to have a qualifier.


The AWS SDK for Kotlin does NOT follow strict [semantic versioning](https://semver.org/). Patch releases may contain new features in addition to bug fixes. See the FAQ for rationale.

## Stability of the AWS SDK for Kotlin

For information about maintenance and support of SDK major versions and their underlying dependencies, see the
following in the AWS SDKs and Tools Shared Configuration and Credentials Reference Guide:

* [AWS SDKs and Tools Maintenance Policy](https://docs.aws.amazon.com/credref/latest/refdocs/maint-policy.html)
* [AWS SDKs and Tools Version Support Matrix](https://docs.aws.amazon.com/credref/latest/refdocs/version-support-matrix.html)


### Qualifiers

Qualifiers for published artifacts

**`-alpha`** indicates:

* The SDK is not meant for production workloads. It is released solely for the purpose of getting feedback.
* The SDK is not yet feature complete. It may contain bugs and performance issues.
* Expect migration issues as APIs/types change.

**`-beta`** indicates:

* The SDK is not meant for production workloads. 
* The SDK is feature complete. It may still contain bugs and performance issues.
* The APIs/types are mostly stabilized. It is still possible that future releases may cause migration issues.

NOTE: This corresponds to the "Developer Preview" phase of the maintenance policy linked above.


## Component Versioning

The SDK versions all service clients (e.g. `S3`, `EC2`, `DynamoDb`, etc) and the runtime (e.g. `aws-config`) together under a single version. This allows customers to easily upgrade multiple SDK clients at once and keep dependencies, such as the core runtime, compatible. The SDK may in the future consider versioning service clients separately from one another.

## Kotlin Versions

The SDK is tested with and provides support for the latest version of Kotlin available (including Kotlin major versions). It MAY work with previous versions of Kotlin but no support or guarantees are provided. The SDK will increment the `MINOR` version whenever a new Kotlin version is adopted. The release notes will clearly state the new version usage and that this may not be a backwards compatible change for customers (e.g. a Gradle plugin that uses the SDK only has whatever version of Kotlin is bundled with that version of Gradle available to it).

NOTE: Upgrading the version of Kotlin the SDK is compiled with does not necessarily mean that new language features or other backwards incompatible language changes are immediately adopted by the SDK.

## Internal APIs

Any API marked with either `@InternalSdkApi` or `@InternalApi` is not subject to any backwards compatibility guarantee. These are meant to be consumed only by the SDK and may be changed or removed without notice. The SDK MAY bump the `MINOR` version when making such a change.

## FAQ

**Why does the SDK not follow semantic versioning (semver)?**

Under semver we’d NEVER use the `PATCH` component. The SDK does *daily* releases containing updates to AWS service models. These changes go out with bug fixes every day. That means for us and customers the `PATCH` version would just be useless, it would always be zero. The intention behind this versioning scheme is to allow customers to weigh the relative risk and/or cost associated with updating to a newer SDK version.

Additionally this follows the existing AWS SDK for Java v2 versioning scheme that many customers coming to the AWS SDK for Kotlin are used to. 

