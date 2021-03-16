# Changelog

## [Unreleased]

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