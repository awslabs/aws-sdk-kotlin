# Requirements Clarification

This document captures the interactive requirements clarification process for the Custom SDK build Gradle plugin.

## Question 1: Target User Experience and Build Integration

**Question:** How should the plugin integrate with existing Gradle build workflows? Specifically, should the custom SDK generation happen:

A) During the standard Gradle build process (e.g., as part of `compileKotlin` or a dedicated task that runs before compilation)
B) As a separate, explicit task that users run manually when they want to regenerate their custom SDK
C) Both - with automatic generation during builds but also allowing manual regeneration
D) Something else entirely

This affects how users will experience the plugin and whether they need to think about when SDK generation occurs versus having it happen transparently.

**Answer:** During the standard Gradle build process as a dedicated task which runs before Kotlin compilation. This provides a seamless developer experience where the custom SDK is automatically generated whenever needed during normal build workflows.

## Question 2: Dependency Management and Artifact Handling

**Question:** How should the plugin handle the generated SDK artifacts in terms of dependency management? Should the plugin:

A) Generate source code directly into the user's source directories (e.g., `src/main/kotlin`) so it becomes part of their project
B) Create a separate generated source set that gets compiled alongside user code
C) Generate and compile the custom SDK into a JAR that gets added as a dependency
D) Use Gradle's composite build feature to create a separate subproject

This affects how users will see and interact with the generated code, as well as how it integrates with their IDE and build caching.

**Answer:** Create a separate generated source set that gets compiled alongside user code. This keeps generated code separate from user code while still allowing seamless compilation and IDE integration.

## Question 3: Operation Selection Mechanism

**Question:** How should users specify which operations they want to include? Looking at your example DSL, I see `operations(GetObject, PutObject)`, but should the plugin also support:

A) String-based operation names: `operations("GetObject", "PutObject")`
B) Pattern matching: `operations("Get*", "Put*")` or `excludeOperations("Delete*")`
C) Operation categories or groups: `operations(READ_OPERATIONS, WRITE_OPERATIONS)`
D) All of the above with different syntax options

Also, should there be a way to include ALL operations for a service (essentially equivalent to the full pre-built client) without having to list every operation individually?

**Answer:** Keep it simple for now. No groups, patterns, or string-based names for services or operations—just the code-generated DSL with typed operation constants like `operations(GetObject, PutObject)`.

## Question 4: Plugin DSL Structure and Return Value

**Question:** Looking at your example DSL where you have `val sdkDependency = awsCustomSdkBuild { ... }`, what should this configuration block actually return? Should it:

A) Return a Gradle dependency notation that can be used directly in `dependencies { implementation(sdkDependency) }`
B) Configure the plugin internally and return nothing (void), with the plugin automatically adding the generated code to the compilation classpath
C) Return a configuration object that users can further customize before applying
D) Something else

This affects both the user experience and the internal plugin architecture.

**Answer:** Return a Gradle dependency notation that can be used directly in `dependencies { implementation(sdkDependency) }`.

## Question 5: Error Handling and Validation

**Question:** How should the plugin handle validation and error scenarios? For example:

A) What should happen if a user specifies an operation that doesn't exist for a service?
B) Should the plugin validate the operation list at configuration time or build time?
C) How should the plugin handle cases where Smithy models change between SDK versions?
D) Should there be any warnings or suggestions when users select operations that have heavy dependencies?

This affects the robustness and user experience of the plugin, especially during development and upgrades.

**Answer:** Since users can only select operations via code-generated constants the compiler will verify that they've selected the names appropriately. Non-existent or misspelled operations will fail to compile during Gradle configuration.

## Question 6: Plugin Distribution and Versioning

**Question:** How should the plugin be distributed and versioned in relation to the main AWS SDK for Kotlin? Should the plugin:

A) Be published as a separate Gradle plugin with its own versioning scheme
B) Be included as part of the main AWS SDK for Kotlin repository and released with the same version numbers
C) Be published separately but maintain strict version alignment with the SDK (e.g., plugin version 1.2.3 only works with SDK 1.2.3)
D) Have some flexibility in version compatibility (e.g., plugin 1.2.x works with SDK 1.2.y where x and y can differ)

This affects how users will manage dependencies and upgrades, as well as the development and release process.

**Answer:** Be included as part of the main AWS SDK for Kotlin repository and released with the same version numbers.

## Question 7: Build Performance and Caching

**Question:** How should the plugin handle build performance and caching considerations? Specifically:

A) Should the plugin support Gradle's build cache for generated code (so identical configurations across different projects can reuse generated artifacts)?
B) How should the plugin handle incremental builds - should it regenerate code only when the configuration changes?
C) Should there be any size or complexity limits to prevent users from accidentally creating massive custom SDKs?
D) How should the plugin handle situations where multiple custom SDK configurations exist in the same project?

This affects build performance, especially in CI/CD environments and large projects.

**Answer:** The plugin should support Gradle's build cache for generated code and should regenerate code only when the configuration changes. No size/complexity limits should be necessary.
