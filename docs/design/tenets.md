# AWS SDK for Kotlin Design Tenets

These are the tenets that drive the development of the AWS SDK for Kotlin in order of priority.

1. **Make simple things simple and complex things possible** - The AWS SDK for Kotlin should provide access to AWS services with obvious, flexible, and convenient APIs.  Where possible the SDK should reduce unneeded complexity in support of common use cases, but only in ways that do not prevent customers from realizing more complex use cases.
2. **Kotlin idiomatic** - A Kotlin SDK should be geared towards developers working in Kotlin. While Kotlin has bindings and interoperability with other languages, the primary use case will be Kotlin applications.
3. **Multi-platform** - One of the key benefits of Kotlin is that it enables the ability to “write-once, run in many places”. The AWS SDK for Kotlin should support customers use cases across the set of stable Kotlin target platforms.
