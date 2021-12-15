# AWS SDK for Kotlin Design Tenets

These are the tenets that drive the development of the AWS SDK for Kotlin in order of priority.

1. **Customer simplicity (aka Don’t Make Me Think)** - Kotlin SDK customers shouldn’t need to decide between different variants of the SDK.  Rather, there is a single AWS Kotlin SDK providing support for all customer use cases with functionality on par with other AWS SDKs.  The SDK itself should provide access to AWS services with obvious, flexible, and convienent APIs.
2. **Mobile is a full citizen** - The world is mobile and customers expect mobile use-cases to be represented with equal weight to their server-based counterparts.
3. **Kotlin idiomatic** - A Kotlin SDK should be geared towards developers working in Kotlin. While Kotlin has bindings and interoperability with other languages, the primary use case will be Kotlin applications.
4. **Multi-platform** - One of the key benefits of Kotlin is that it enables the ability to “write-once, run in many places”. The AWS SDK for Kotlin should support customers use cases across the set of stable Kotlin target platforms.
