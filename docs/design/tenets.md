# AWS SDK for Kotlin Design Tenets


These are the tenets that drive the development of the AWS Kotlin SDK in priority order.


1. **Customer simplicity (aka Don’t Make Me Think)** - the AWS ecosystem is split into 1000s of teams - but that doesn’t mean that customers should have to care. In the case of the Kotlin SDK customers shouldn’t need to decide between a “mobile” version and a “server” version of the SDK: there should be a single low-level AWS Kotlin SDK, that is consistent in style and capabilities to any other supported AWS SDK.
2. **Mobile is a full citizen** - the world is mobile and customers expect mobile use-cases to be represented with equal weight to their server-based counterparts.
3. **Kotlin idiomatic** - a Kotlin SDK should be geared towards developers working in Kotlin. While it’s true Kotlin has very good Java inter-op, as well as bindings to JS, Native and objc/SWIFT - the primary target customer will be pure-Kotlin applications. This means that Kotlin-only concepts like coroutines and rich DSLs can and should be part of the primary API; with wrappers for other runtimes as appropriate.
4. **Multi-platform** - one of the key benefits of Kotlin is that it enables the ability to “write-once, run in many places”. The AWS SDK for Kotlin should support customers use-cases across the set of stable Kotlin target platforms.
