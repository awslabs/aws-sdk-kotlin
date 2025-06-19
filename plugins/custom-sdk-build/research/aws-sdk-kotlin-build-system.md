# AWS SDK for Kotlin Build System Research

## Overview
Research into the current AWS SDK for Kotlin build system, codegen processes, and plugin architecture.

## Current Build Structure

### Key Components
1. **codegen/sdk** - Main codegen orchestration
2. **buildSrc** - Build logic and plugins
3. **build-support** - Additional build utilities
4. **services/** - Generated service clients

### Codegen Process
Based on `codegen/sdk/build.gradle.kts`:

1. **Service Discovery**: `discoverServices()` function reads JSON model files from `aws-models/` directory
2. **Smithy Projections**: Each service gets a `SmithyProjection` with:
   - Model imports (service model + extras)
   - Transforms (including `awsSmithyKotlinIncludeOperations`)
   - Package settings
3. **Code Generation**: Uses `smithyBuild.projections` to generate code
4. **Staging**: `stageSdks` task copies generated code to `services/<service>/generated-src/`

### Key Insights for Plugin Design

#### Smithy Integration
- Already uses Smithy projections with transforms
- `awsSmithyKotlinIncludeOperations` transform exists for operation filtering
- Models are loaded from JSON files in `aws-models/` directory

#### Build Configuration
- Services are configured via `AwsService` data class
- Package settings stored in `package.json` files per service
- Transforms can be applied per service

#### Generated Code Management
- Generated code goes to `services/<service>/generated-src/`
- Each service gets its own `build.gradle.kts`
- Services are registered with the build dynamically

## Implications for Custom SDK Plugin

### Reusable Components
1. **AwsService model** - Can be adapted for custom configurations
2. **Smithy projection setup** - Similar pattern for custom projections
3. **Transform system** - `awsSmithyKotlinIncludeOperations` is exactly what we need

### Plugin Architecture Considerations
1. Plugin should integrate with existing Smithy build infrastructure
2. Can reuse model loading and projection creation patterns
3. Need to create custom source sets instead of separate service directories
4. Should leverage existing transform system for operation filtering

## Next Steps
- Examine `awsSmithyKotlinIncludeOperations` transform implementation
- Research Gradle source set creation patterns
- Investigate dependency notation creation mechanisms
