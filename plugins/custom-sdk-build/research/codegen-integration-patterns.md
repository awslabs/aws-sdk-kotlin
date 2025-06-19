# Codegen Integration Patterns Research

## Overview
Research into how to properly integrate with the existing Smithy Kotlin codegen system using `KotlinIntegration` and SPI rather than creating separate codegen logic.

## Key Findings

### KotlinIntegration System
- **Location**: `smithy-kotlin/codegen/smithy-kotlin-codegen/src/main/kotlin/software/amazon/smithy/kotlin/codegen/integration/KotlinIntegration.kt`
- **Purpose**: JVM SPI for customizing Kotlin code generation, registering protocol generators, modifying models, adding custom code, etc.
- **Discovery**: Integrations are discovered via SPI using `META-INF/services/software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration`

### Integration Capabilities
Key methods available in `KotlinIntegration`:

1. **`writeAdditionalFiles()`**: Write additional files during codegen (perfect for DSL generation)
2. **`preprocessModel()`**: Modify the model before code generation
3. **`decorateSymbolProvider()`**: Customize symbol names, packages, dependencies
4. **`enabledForService()`**: Control which services the integration applies to
5. **`order`**: Control execution order of integrations

### AWS SDK Integration Examples
From `aws-sdk-codegen/src/main/resources/META-INF/services/`:

- **`ModuleDocumentationIntegration`**: Generates `OVERVIEW.md` files
- **`GradleGenerator`**: Generates `build.gradle.kts` files for services
- **Service-specific integrations**: S3, Glacier, etc. customizations

### SPI Registration Pattern
Integrations are registered in `META-INF/services/software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration`:
```
aws.sdk.kotlin.codegen.ModuleDocumentationIntegration
aws.sdk.kotlin.codegen.GradleGenerator
# ... other integrations
```

## Implications for Custom SDK Plugin

### Revised Architecture
Instead of creating separate codegen logic, the plugin should:

1. **Create a `KotlinIntegration`** that generates the DSL code during normal SDK codegen
2. **Use existing model loading** - no need to read AWS models separately
3. **Leverage existing service discovery** - reuse `AwsService` and related utilities
4. **Generate DSL during SDK build** - DSL code becomes part of the plugin artifact

### Integration Implementation Strategy

```kotlin
class CustomSdkDslGeneratorIntegration : KotlinIntegration {
    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        // Generate service DSL classes and operation constants
        generateServiceDslClasses(ctx, delegator)
        generateOperationConstants(ctx, delegator)
    }
    
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        // Only run for a special "dsl-generator" service or all services
        return true
    }
}
```

### Plugin Build Process
1. **Plugin build time**: Run codegen with the DSL generator integration to create service DSL code
2. **Plugin compilation**: Compile generated DSL code as part of plugin artifact
3. **User build time**: Plugin uses pre-generated DSL to configure custom SDK generation

### Benefits of This Approach
- **Reuses existing infrastructure**: Model loading, service discovery, symbol providers
- **Consistent with SDK patterns**: Same codegen system used throughout
- **Automatic updates**: DSL regenerates when models change
- **No duplicate logic**: Leverages existing `AwsService` utilities and model processing

## Next Steps
- Design the `CustomSdkDslGeneratorIntegration` implementation
- Determine how to trigger DSL generation during plugin build
- Update plugin architecture to use pre-generated DSL code
- Revise the overall design to align with this approach
