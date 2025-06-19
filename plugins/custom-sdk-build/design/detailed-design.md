# Custom SDK Build Gradle Plugin - Detailed Design

## Overview

The Custom SDK Build Gradle plugin enables users to generate custom AWS SDK clients containing only the operations they need, reducing binary size and improving startup times. The plugin integrates seamlessly with existing Gradle build workflows by generating code during the standard build process and creating a separate source set that compiles alongside user code.

## Requirements

Based on the requirements clarification process, the plugin must:

1. **Build Integration**: Generate custom SDK during standard Gradle build process via dedicated task before Kotlin compilation
2. **Artifact Management**: Create separate generated source set compiled alongside user code
3. **Operation Selection**: Support simple code-generated DSL with typed operation constants
4. **DSL Structure**: Return Gradle dependency notation for use in dependencies block
5. **Error Handling**: Rely on compiler validation through code-generated constants
6. **Distribution**: Be part of main AWS SDK for Kotlin repository with matching version numbers
7. **Performance**: Support Gradle build cache and incremental builds with no artificial limits

## Architecture

### High-Level Architecture

```
User Project
├── build.gradle.kts
│   ├── plugins { id("aws.sdk.kotlin.custom-sdk-build") }
│   ├── val customSdk = awsCustomSdkBuild { ... }
│   └── dependencies { implementation(customSdk) }
├── src/main/kotlin/ (user code)
└── build/
    └── generated/sources/
        └── customSdk/ (generated SDK code)
```

### Component Architecture

1. **Plugin Core** (`CustomSdkBuildPlugin`)
   - Registers plugin extension and tasks
   - Configures generated source sets
   - Returns dependency notation

2. **DSL Extension** (`CustomSdkBuildExtension`)
   - Provides `awsCustomSdkBuild { }` DSL
   - Collects service and operation selections
   - Validates configuration

3. **Code Generation Task** (`GenerateCustomSdkTask`)
   - Creates Smithy projections with `awsSmithyKotlinIncludeOperations` transform
   - Generates filtered SDK code
   - Manages incremental builds and caching

4. **DSL Generation Integration** (`CustomSdkDslGeneratorIntegration`)
   - Implements `KotlinIntegration` to generate DSL code during plugin build
   - Leverages existing AWS SDK codegen infrastructure
   - Generates typed service methods and operation constants

## Components and Interfaces

### Plugin Core

```kotlin
class CustomSdkBuildPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Register extension
        val extension = project.extensions.create("awsCustomSdkBuild", CustomSdkBuildExtension::class.java, project)
        
        // Configure generated source set
        configureGeneratedSourceSet(project)
        
        // Register generation task
        val generateTask = project.tasks.register("generateCustomSdk", GenerateCustomSdkTask::class.java)
        
        // Configure task dependencies
        configureTaskDependencies(project, generateTask)
        
        // Return dependency notation
        return project.dependencies.create(project.files(generatedSourceDir))
    }
}
```

### DSL Extension

```kotlin
open class CustomSdkBuildExtension(private val project: Project) {
    private val serviceConfigurations = mutableMapOf<String, ServiceConfiguration>()
    
    // Generated service methods (example)
    fun s3(configure: S3ServiceConfiguration.() -> Unit) {
        val config = S3ServiceConfiguration().apply(configure)
        serviceConfigurations["s3"] = config
    }
    
    fun dynamodb(configure: DynamoDbServiceConfiguration.() -> Unit) {
        val config = DynamoDbServiceConfiguration().apply(configure)
        serviceConfigurations["dynamodb"] = config
    }
    
    internal fun getSelectedOperations(): Map<String, List<String>> {
        return serviceConfigurations.mapValues { (_, config) -> 
            config.selectedOperations.map { it.shapeId }
        }
    }
}
```

### DSL Generation Integration (New Approach)

```kotlin
class CustomSdkDslGeneratorIntegration : KotlinIntegration {
    
    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        // Only generate DSL for plugin build, not regular service builds
        if (!isPluginBuild(ctx)) return
        
        // Generate service DSL classes and operation constants
        generateServiceDslClasses(ctx, delegator)
        generateOperationConstants(ctx, delegator)
    }
    
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        // Enable for all services during plugin DSL generation
        return isPluginBuild(settings)
    }
    
    private fun generateServiceDslClasses(ctx: CodegenContext, delegator: KotlinDelegator) {
        val service = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        val serviceTrait = service.getTrait<ServiceTrait>().orNull()
            ?: return
        
        val serviceName = serviceTrait.sdkId.lowercase()
        val operations = service.allOperations.map { operationId ->
            val operation = ctx.model.expectShape<OperationShape>(operationId)
            OperationMetadata(
                name = operation.id.name,
                shapeId = operation.id.toString()
            )
        }
        
        // Generate service configuration class
        delegator.useFileWriter("${serviceName}ServiceConfiguration.kt") { writer ->
            generateServiceConfigurationClass(writer, serviceName, operations)
        }
        
        // Generate operation constants
        delegator.useFileWriter("${serviceName}Operations.kt") { writer ->
            generateOperationConstants(writer, serviceName, operations)
        }
    }
    
    private fun isPluginBuild(ctx: CodegenContext): Boolean {
        // Detect if this is a plugin DSL generation build vs regular service build
        return ctx.settings.pkg.name.contains("custom-sdk-build")
    }
}
```

### Service Configuration Classes (Generated by Integration)

```kotlin
// Generated for each service by CustomSdkDslGeneratorIntegration
class S3ServiceConfiguration {
    internal val selectedOperations = mutableListOf<OperationConstant>()
    
    fun operations(vararg operations: S3Operation) {
        selectedOperations.addAll(operations)
    }
}

// Generated operation constants
object S3Operation {
    val GetObject = OperationConstant("com.amazonaws.s3#GetObject")
    val PutObject = OperationConstant("com.amazonaws.s3#PutObject")
    // ... other operations
}

data class OperationConstant(val shapeId: String)
```

### Code Generation Task

```kotlin
@CacheableTask
abstract class GenerateCustomSdkTask : DefaultTask() {
    
    @get:Input
    abstract val selectedOperations: MapProperty<String, List<String>>
    
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    
    @TaskAction
    fun generate() {
        val operations = selectedOperations.get()
        val allOperationShapeIds = operations.values.flatten()
        
        // Create Smithy projection with IncludeOperations transform
        val projection = createSmithyProjection(allOperationShapeIds)
        
        // Generate code using smithy-build
        executeSmithyBuild(projection)
    }
    
    private fun createSmithyProjection(operations: List<String>): SmithyProjection {
        return SmithyProjection("custom-sdk").apply {
            imports = getRequiredModelFiles()
            transforms = listOf(createIncludeOperationsTransform(operations))
            smithyKotlinPlugin {
                packageName = "aws.sdk.kotlin.services.custom"
                packageVersion = project.version.toString()
                // ... other settings
            }
        }
    }
    
    private fun createIncludeOperationsTransform(operations: List<String>): String {
        return """
        {
            "name": "awsSmithyKotlinIncludeOperations",
            "args": {
                "operations": ${operations.joinToString(prefix = "[\"", postfix = "\"]", separator = "\", \"")}
            }
        }
        """.trimIndent()
    }
}
```

## Data Models

### Service Configuration Model

```kotlin
data class CustomSdkConfiguration(
    val services: Map<String, ServiceSelection>,
    val packageName: String = "aws.sdk.kotlin.services.custom",
    val outputDirectory: String = "build/generated/sources/customSdk"
)

data class ServiceSelection(
    val serviceName: String,
    val operations: List<String>, // Smithy shape IDs
    val namespace: String // e.g., "com.amazonaws.s3"
)
```

### Operation Mapping Model

```kotlin
data class ServiceMetadata(
    val serviceName: String,
    val namespace: String,
    val modelFile: String,
    val operations: List<OperationMetadata>
)

data class OperationMetadata(
    val name: String, // e.g., "GetObject"
    val shapeId: String // e.g., "com.amazonaws.s3#GetObject"
)
```

## Error Handling

### Compile-Time Validation
- **Operation Selection**: Invalid operations fail at Gradle configuration time due to typed constants
- **Service Selection**: Invalid services fail at Gradle configuration time due to typed DSL methods
- **Configuration**: Missing required configuration fails during task execution with clear error messages

### Runtime Error Handling
- **Model Loading**: Clear error messages if AWS models cannot be loaded
- **Transform Execution**: Detailed error reporting if Smithy transform fails
- **Code Generation**: Comprehensive error reporting for codegen failures

### Error Recovery
- **Incremental Builds**: Failed generation doesn't break subsequent builds
- **Partial Failures**: Clear indication of which services/operations failed
- **Validation**: Early validation prevents expensive generation attempts

## Testing Strategy

### Unit Tests
- **Plugin Registration**: Verify plugin applies correctly and registers extensions
- **DSL Validation**: Test service and operation selection DSL
- **Transform Configuration**: Verify correct JSON generation for IncludeOperations transform
- **Task Configuration**: Test task dependencies and incremental build behavior

### Integration Tests
- **End-to-End Generation**: Full plugin execution with sample configurations
- **Multi-Service**: Test combinations of multiple services and operations
- **Build Cache**: Verify build cache integration works correctly
- **Incremental Builds**: Test that only changed configurations trigger regeneration

### Functional Tests
- **Generated Code Quality**: Verify generated SDK clients work correctly
- **Dependency Resolution**: Test that all required dependencies are included
- **IDE Integration**: Verify generated code appears correctly in IDEs

### Performance Tests
- **Generation Speed**: Measure code generation performance with various configurations
- **Build Cache Effectiveness**: Measure cache hit rates and performance improvements
- **Memory Usage**: Monitor memory consumption during generation

## Implementation Details

### Plugin Build-Time DSL Generation

The plugin uses a `KotlinIntegration` to generate DSL code during the plugin's own build process:

1. **Integration Registration**: `CustomSdkDslGeneratorIntegration` is registered via SPI
2. **Service Discovery**: Leverages existing AWS SDK service discovery and model loading
3. **DSL Generation**: Generates typed service configuration classes and operation constants
4. **Plugin Compilation**: Generated DSL code is compiled as part of the plugin artifact

### Integration with Existing Infrastructure

```kotlin
// Leverages existing AwsService discovery
private fun discoverAwsServices(ctx: CodegenContext): List<AwsService> {
    // Reuse existing service discovery logic from aws-sdk-kotlin
    return AwsServiceDiscovery.discoverServices(ctx.model, ctx.settings)
}

// Uses existing service trait processing
private fun extractServiceMetadata(service: ServiceShape): ServiceMetadata {
    val serviceTrait = service.getTrait<ServiceTrait>().orNull()
        ?: error("Service missing AWS service trait")
    
    return ServiceMetadata(
        serviceName = serviceTrait.sdkId,
        namespace = service.id.namespace,
        operations = service.allOperations.map { /* ... */ }
    )
}
```

### Source Set Integration

```kotlin
private fun configureGeneratedSourceSet(project: Project) {
    project.plugins.withType<KotlinMultiplatformPlugin> {
        project.kotlin.sourceSets.commonMain {
            kotlin.srcDir(generatedSourceDir)
        }
    }
    
    // Configure task dependencies
    project.tasks.withType<KotlinCompile> {
        dependsOn("generateCustomSdk")
    }
}
```

### Build Cache Integration

```kotlin
@CacheableTask
abstract class GenerateCustomSdkTask : DefaultTask() {
    
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val modelFiles: ConfigurableFileCollection
    
    @get:Input
    abstract val selectedOperations: MapProperty<String, List<String>>
    
    @get:Input
    abstract val packageName: Property<String>
    
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
}
```

## Deployment and Distribution

### Plugin Publication
- Published as part of AWS SDK for Kotlin releases
- Same version number as SDK runtime components
- Available through Gradle Plugin Portal and Maven Central

### Version Compatibility
- Plugin version must match SDK runtime version exactly
- Models embedded in plugin match corresponding SDK release
- Clear error messages for version mismatches

### Documentation
- Plugin usage guide in main SDK documentation
- Code examples and common patterns
- Migration guide from full SDK clients

## Security Considerations

### Model Access
- Plugin accesses same AWS models as main SDK build
- No additional security concerns beyond existing SDK build process

### Generated Code
- Generated code follows same security patterns as main SDK
- No credentials or sensitive data in generated artifacts
- Standard AWS SDK security practices apply

## Performance Considerations

### Build Performance
- Incremental generation based on configuration changes
- Build cache support for identical configurations across projects
- Parallel generation for multiple services when possible

### Runtime Performance
- Generated clients have same performance characteristics as full SDK clients
- Reduced binary size improves startup time
- No runtime overhead compared to equivalent full SDK usage

### Memory Usage
- Generation process memory usage scales with number of selected operations
- No artificial limits on configuration size
- Efficient model loading and processing

## Future Enhancements

### Potential Extensions
- Pattern-based operation selection (e.g., "Get*", "Put*")
- Operation grouping and categories
- Custom package naming per service
- Integration with dependency analysis tools

### Compatibility
- Design allows for backward-compatible addition of new features
- Extensible DSL structure supports future enhancements
- Plugin architecture supports additional transforms and customizations
