# IncludeOperations Transform Implementation Research

## Overview
Research into the actual implementation of `IncludeOperations.kt` in smithy-kotlin to understand how operation filtering works internally.

## Implementation Analysis

### Transform Structure
The `IncludeOperations` transform is implemented as a `ConfigurableProjectionTransformer` with the following key characteristics:

```kotlin
class IncludeOperations : ConfigurableProjectionTransformer<IncludeOperations.Config>() {
    class Config {
        var operations: Set<String> = emptySet()
    }
    
    override fun getName(): String = "awsSmithyKotlinIncludeOperations"
    override fun getConfigType(): Class<Config> = Config::class.java
    
    override fun transformWithConfig(context: TransformContext, config: Config): Model {
        check(config.operations.isNotEmpty()) { "no operations provided to IncludeOperations transform!" }
        return context.transformer.filterShapes(context.model) { shape ->
            when (shape) {
                is OperationShape -> shape.id.toString() in config.operations
                else -> true
            }
        }
    }
}
```

### Key Implementation Details

1. **Configuration Format**: 
   - Takes a `Config` object with an `operations` property of type `Set<String>`
   - Operations are specified as string shape IDs (e.g., "com.amazonaws.s3#GetObject")

2. **Filtering Logic**:
   - Uses Smithy's built-in `filterShapes` method
   - Only filters `OperationShape` instances - all other shapes pass through
   - Operations are included if their shape ID string is in the configured set

3. **Dependency Resolution**:
   - The transform relies on Smithy's `filterShapes` method for dependency resolution
   - Non-operation shapes (data types, errors, etc.) are automatically included if referenced by included operations
   - This means the transform handles transitive dependencies automatically

4. **Validation**:
   - Validates that at least one operation is provided
   - No validation of operation shape ID format or existence

## Implications for Custom SDK Plugin

### Transform Configuration
- Plugin needs to convert user's operation selections to full Smithy shape IDs
- Shape IDs follow the format: `{namespace}#{operationName}` (e.g., "com.amazonaws.s3#GetObject")
- Configuration is passed as JSON with an "operations" array

### Dependency Handling
- Transform automatically includes all required data types, error types, and other dependencies
- No need for plugin to manually track or include dependencies
- Shared types between included and excluded operations are handled correctly

### Operation Shape ID Mapping
- Plugin needs to map from operation constants (e.g., `GetObject`) to full shape IDs
- This requires knowledge of the service namespace for each service
- Shape ID format: `{service.namespace}#{operation.name}`

### Error Handling
- Transform will fail if no operations are provided
- No validation of operation existence - invalid shape IDs will be silently ignored
- Plugin should validate operation selections before passing to transform

## Integration Requirements

### For Custom SDK Plugin
1. **Service Namespace Resolution**: Plugin needs to determine the correct namespace for each service
2. **Operation Constant Mapping**: Map user's operation constants to shape ID strings
3. **Transform Configuration**: Generate proper JSON configuration for the transform
4. **Validation**: Validate operation selections before transform execution

### Example Transform Configuration
```json
{
    "name": "awsSmithyKotlinIncludeOperations",
    "args": {
        "operations": [
            "com.amazonaws.s3#GetObject",
            "com.amazonaws.s3#PutObject",
            "com.amazonaws.dynamodb#GetItem",
            "com.amazonaws.dynamodb#PutItem"
        ]
    }
}
```

## Next Steps
- Research how to determine service namespaces from Smithy models
- Investigate operation constant to shape ID mapping strategies
- Understand how to integrate this transform configuration into plugin projections
