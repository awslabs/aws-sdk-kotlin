# Implementation Prompt Plan

## Checklist
- [x] Prompt 1: Set up plugin project structure and basic Gradle plugin
- [x] Prompt 2: Create KotlinIntegration for DSL generation
- [x] Prompt 3: Implement service discovery and operation extraction
- [x] Prompt 4: Generate service configuration classes and operation constants
- [x] Prompt 5: Create plugin core and DSL extension
- [x] Prompt 6: Implement custom SDK generation task
- [x] Prompt 7: Add source set integration and task dependencies
- [x] Prompt 8: Implement build cache support and incremental builds
- [x] Prompt 9: Add comprehensive error handling and validation
- [x] Prompt 10: Create integration tests and end-to-end validation
- [x] Prompt 11: Add plugin registration and SPI configuration
- [ ] Prompt 12: Wire everything together and create final integration

## Git Checkpointing Instructions

### Git Checkpointing
After completing each prompt, create a git commit with all src and test code changes:

**Constraints:**
- You MUST add and commit all changes in src/ and test/ directories
- You MUST NOT commit files in plugins/custom-sdk-build/ directory or build artifacts
- You MUST use descriptive commit messages following this format:
  ```
  feat: implement [brief description of completed work]

  - [Key achievement 1]
  - [Key achievement 2]
  - [Key achievement 3]

  Completes Prompt N of implementation plan
  ```
- You MUST verify the git status before committing
- You MUST assume the user has properly set up their git environment and branch

**Git Commands:**
```bash
# Add source and test files only
git add src/ test/

# Commit with descriptive message
git commit -m "feat: implement [description]

- [achievement 1]
- [achievement 2]

Completes Prompt N of implementation plan"
```

## Prompts

### Prompt 1: Set up plugin project structure and basic Gradle plugin

Create the basic project structure for the custom SDK build Gradle plugin within the existing AWS SDK for Kotlin repository structure. Set up the plugin module with proper Gradle configuration, dependencies, and basic plugin class.

1. Create the plugin module directory structure under `plugins/custom-sdk-build/`
2. Set up `build.gradle.kts` with `kotlin-dsl` and `java-gradle-plugin` plugins
3. Configure dependencies on existing AWS SDK build utilities and Smithy libraries
4. Create the basic `CustomSdkBuildPlugin` class that implements `Plugin<Project>`
5. Register the plugin in the `gradlePlugin` block with ID `aws.sdk.kotlin.custom-sdk-build`
6. Add the plugin module to the root `settings.gradle.kts`

Focus on establishing a clean foundation that integrates properly with the existing repository structure. The plugin should apply successfully but doesn't need to do anything functional yet.

**Git Checkpoint**: After completing this prompt, commit all changes with:
```bash
git add src/ test/
git commit -m "feat: implement basic plugin project structure

- Created plugin module under plugins/custom-sdk-build/
- Set up Gradle configuration with kotlin-dsl and java-gradle-plugin
- Implemented basic CustomSdkBuildPlugin class
- Registered plugin with ID aws.sdk.kotlin.custom-sdk-build

Completes Prompt 1 of implementation plan"
```

### Prompt 2: Create KotlinIntegration for DSL generation

Implement the `CustomSdkDslGeneratorIntegration` class that extends `KotlinIntegration` to generate DSL code during the plugin build process. This integration will discover AWS services and generate typed service configuration classes.

1. Create `CustomSdkDslGeneratorIntegration` class implementing `KotlinIntegration`
2. Implement `writeAdditionalFiles()` to generate DSL code during codegen
3. Add logic to detect when this is a plugin DSL generation build vs regular service build
4. Implement `enabledForService()` to control when the integration runs
5. Add service discovery logic that leverages existing AWS SDK infrastructure
6. Create basic file generation structure using `KotlinDelegator`

The integration should be able to identify AWS services and prepare for DSL generation, but doesn't need to generate the actual DSL code yet.

**Git Checkpoint**: After completing this prompt, commit all changes with:
```bash
git add src/ test/
git commit -m "feat: implement KotlinIntegration for DSL generation

- Created CustomSdkDslGeneratorIntegration extending KotlinIntegration
- Implemented writeAdditionalFiles and enabledForService methods
- Added plugin build detection logic
- Set up basic service discovery framework

Completes Prompt 2 of implementation plan"
```

### Prompt 3: Implement service discovery and operation extraction

Enhance the `CustomSdkDslGeneratorIntegration` to properly discover AWS services and extract operation metadata using existing AWS SDK utilities and Smithy model processing.

1. Implement service discovery using existing `AwsService` utilities and patterns
2. Extract service metadata including service name, namespace, and SDK ID
3. Discover all operations for each service using Smithy model traversal
4. Create operation metadata with operation names and full Smithy shape IDs
5. Handle service traits and AWS-specific service information properly
6. Add proper error handling for missing or invalid service models

Leverage the existing codegen infrastructure to avoid duplicating model loading and service discovery logic. The integration should be able to discover all AWS services and their operations.

**Git Checkpoint**: After completing this prompt, commit all changes with:
```bash
git add src/ test/
git commit -m "feat: implement service discovery and operation extraction

- Added service discovery using existing AwsService utilities
- Implemented operation metadata extraction from Smithy models
- Added service trait processing for AWS-specific information
- Created proper error handling for invalid service models

Completes Prompt 3 of implementation plan"
```

### Prompt 4: Generate service configuration classes and operation constants

Implement the actual DSL code generation that creates typed service configuration classes and operation constants for each discovered AWS service.

1. Generate service configuration classes (e.g., `S3ServiceConfiguration`) for each service
2. Create operation constant objects (e.g., `S3Operation`) with typed operation constants
3. Generate the main DSL extension methods (e.g., `fun s3(configure: S3ServiceConfiguration.() -> Unit)`)
4. Ensure proper Kotlin code generation with correct imports and package structure
5. Handle service name normalization and Kotlin naming conventions
6. Generate `OperationConstant` data class with Smithy shape IDs

The generated code should provide a type-safe DSL that prevents configuration errors and enables IDE autocompletion.

**Git Checkpoint**: After completing this prompt, commit all changes with:
```bash
git add src/ test/
git commit -m "feat: implement DSL code generation

- Generated service configuration classes for each AWS service
- Created operation constant objects with typed constants
- Implemented DSL extension method generation
- Added proper Kotlin naming conventions and imports

Completes Prompt 4 of implementation plan"
```

### Prompt 5: Create plugin core and DSL extension

Implement the main plugin components including the `CustomSdkBuildExtension` that provides the user-facing DSL and the core plugin logic that registers extensions and configures the build.

1. Implement `CustomSdkBuildExtension` class with the `awsCustomSdkBuild` DSL
2. Add service configuration collection and validation logic
3. Implement the method that returns selected operations as Smithy shape IDs
4. Create the plugin core logic that registers the extension with the project
5. Add basic validation for user configurations
6. Implement the dependency notation return mechanism

The extension should collect user's service and operation selections and make them available for code generation.

**Git Checkpoint**: After completing this prompt, commit all changes with:
```bash
git add src/ test/
git commit -m "feat: implement plugin core and DSL extension

- Created CustomSdkBuildExtension with awsCustomSdkBuild DSL
- Added service configuration collection and validation
- Implemented operation selection to shape ID mapping
- Created plugin registration and extension setup

Completes Prompt 5 of implementation plan"
```

### Prompt 6: Implement custom SDK generation task

Create the `GenerateCustomSdkTask` that uses Smithy projections and the `awsSmithyKotlinIncludeOperations` transform to generate filtered SDK code based on user selections.

1. Implement `GenerateCustomSdkTask` as a cacheable Gradle task
2. Add proper input/output annotations for Gradle caching
3. Create Smithy projection configuration with the IncludeOperations transform
4. Implement JSON transform configuration generation
5. Add smithy-build execution logic to generate filtered SDK code
6. Handle multiple services and operation combinations properly

The task should generate custom SDK code containing only the operations selected by the user.

**Git Checkpoint**: After completing this prompt, commit all changes with:
```bash
git add src/ test/
git commit -m "feat: implement custom SDK generation task

- Created GenerateCustomSdkTask with proper caching annotations
- Implemented Smithy projection configuration
- Added IncludeOperations transform JSON generation
- Created smithy-build execution logic for filtered SDK generation

Completes Prompt 6 of implementation plan"
```

### Prompt 7: Add source set integration and task dependencies

Implement the source set configuration that integrates generated SDK code with the user's project compilation and sets up proper task dependencies.

1. Configure generated source sets for Kotlin multiplatform projects
2. Add the generated source directory to the appropriate source sets
3. Set up task dependencies so generation runs before Kotlin compilation
4. Handle both JVM and multiplatform project configurations
5. Ensure generated code is properly included in IDE indexing
6. Add proper cleanup and incremental build support

The generated SDK code should compile alongside user code seamlessly.

**Git Checkpoint**: After completing this prompt, commit all changes with:
```bash
git add src/ test/
git commit -m "feat: implement source set integration and task dependencies

- Configured generated source sets for Kotlin projects
- Added proper task dependencies for compilation order
- Implemented multiplatform project support
- Added IDE integration and incremental build support

Completes Prompt 7 of implementation plan"
```

### Prompt 8: Implement build cache support and incremental builds

Enhance the generation task with proper Gradle build cache support and incremental build capabilities to optimize build performance.

1. Add comprehensive input/output annotations for build caching
2. Implement proper path sensitivity for model files and configuration
3. Add incremental build logic that only regenerates when configuration changes
4. Handle build cache key generation based on selected operations
5. Optimize model file loading and processing for performance
6. Add proper task up-to-date checking

The plugin should leverage Gradle's build cache effectively and avoid unnecessary regeneration.

**Git Checkpoint**: After completing this prompt, commit all changes with:
```bash
git add src/ test/
git commit -m "feat: implement build cache support and incremental builds

- Added comprehensive input/output annotations for caching
- Implemented incremental build logic for configuration changes
- Added proper path sensitivity and cache key generation
- Optimized model file processing for performance

Completes Prompt 8 of implementation plan"
```

### Prompt 9: Add comprehensive error handling and validation

Implement robust error handling, validation, and user-friendly error messages throughout the plugin to ensure a good developer experience.

1. Add validation for user configuration at DSL configuration time
2. Implement clear error messages for invalid service or operation selections
3. Add proper error handling for model loading and processing failures
4. Handle edge cases like empty configurations or missing dependencies
5. Provide helpful suggestions when users make configuration mistakes
6. Add logging and debugging support for troubleshooting

The plugin should provide clear, actionable error messages that help users resolve configuration issues quickly.

**Git Checkpoint**: After completing this prompt, commit all changes with:
```bash
git add src/ test/
git commit -m "feat: implement comprehensive error handling and validation

- Added DSL configuration validation with clear error messages
- Implemented robust error handling for model processing
- Added helpful suggestions for configuration mistakes
- Created logging and debugging support

Completes Prompt 9 of implementation plan"
```

### Prompt 10: Create integration tests and end-to-end validation

Implement comprehensive tests that validate the plugin functionality from DSL configuration through code generation and compilation.

1. Create integration tests that apply the plugin to test projects
2. Test various service and operation combinations
3. Validate that generated code compiles and works correctly
4. Test build cache behavior and incremental builds
5. Add tests for error conditions and edge cases
6. Create performance tests for generation speed and memory usage

The tests should ensure the plugin works correctly in realistic scenarios and catches regressions.

**Git Checkpoint**: After completing this prompt, commit all changes with:
```bash
git add src/ test/
git commit -m "feat: implement integration tests and end-to-end validation

- Created comprehensive integration tests for plugin functionality
- Added tests for various service and operation combinations
- Implemented build cache and incremental build testing
- Added error condition and performance tests

Completes Prompt 10 of implementation plan"
```

### Prompt 11: Add plugin registration and SPI configuration

Set up the proper SPI registration for the `CustomSdkDslGeneratorIntegration` and ensure the plugin is properly registered for distribution.

1. Create `META-INF/services/software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration` file
2. Register the `CustomSdkDslGeneratorIntegration` in the SPI configuration
3. Ensure the plugin is properly configured for publication
4. Add version compatibility checks and validation
5. Configure the plugin for inclusion in AWS SDK releases
6. Add proper plugin metadata and documentation

The plugin should be properly integrated with the AWS SDK build and distribution process.

**Git Checkpoint**: After completing this prompt, commit all changes with:
```bash
git add src/ test/
git commit -m "feat: implement plugin registration and SPI configuration

- Added SPI registration for CustomSdkDslGeneratorIntegration
- Configured plugin for AWS SDK distribution
- Added version compatibility checks and validation
- Created proper plugin metadata and documentation

Completes Prompt 11 of implementation plan"
```

### Prompt 12: Wire everything together and create final integration

Complete the implementation by ensuring all components work together seamlessly and create the final integration that enables end-to-end functionality.

1. Ensure the DSL generation integration runs during plugin build
2. Verify that generated DSL code is properly compiled into the plugin artifact
3. Test the complete user workflow from plugin application to custom SDK generation
4. Add final polish including documentation, examples, and usage guides
5. Validate that the plugin integrates properly with existing AWS SDK patterns
6. Perform final testing and validation of all functionality

The plugin should be fully functional and ready for use, providing a seamless experience for generating custom AWS SDK clients.

**Git Checkpoint**: After completing this prompt, commit all changes with:
```bash
git add src/ test/
git commit -m "feat: complete plugin implementation and final integration

- Integrated all components for end-to-end functionality
- Added final documentation and usage examples
- Validated complete user workflow from DSL to custom SDK
- Performed comprehensive testing and validation

Completes Prompt 12 of implementation plan"
```
