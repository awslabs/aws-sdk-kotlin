# AWS Custom SDK Build Plugin

A Gradle plugin for generating lightweight AWS service clients with only selected operations, providing type-safe operation constants and intelligent validation with real Smithy build integration.

## Features

- **Real Smithy Build Integration**: Leverages actual Smithy build process for authentic client generation
- **Multi-Tier Build Strategy**: Gradle plugin integration → Smithy CLI → graceful fallback
- **Type-Safe Operation Selection**: Use generated constants instead of error-prone string literals
- **Intelligent Validation**: Automatic validation of operation names against AWS service definitions
- **Flexible DSL**: Support for both type-safe constants and string literals (backward compatibility)
- **Advanced Selection**: Pattern matching, bulk selection, and filtering capabilities
- **Comprehensive Validation**: Detailed validation reports with suggestions for invalid operations
- **Robust Error Handling**: Graceful degradation when Smithy models or CLI are unavailable

## Architecture

### Real Smithy Build Integration

The plugin implements a sophisticated multi-tier approach to client generation:

1. **Primary**: Gradle Smithy Build Plugin Integration
   - Uses AWS Kotlin repo tools when available
   - Leverages existing AWS SDK build infrastructure
   
2. **Secondary**: Direct Smithy CLI Execution
   - Executes Smithy CLI with generated `smithy-build.json`
   - Automatic JAR discovery across multiple locations
   - Configurable build parameters and output paths
   
3. **Fallback**: Development/Testing Mode
   - Generates placeholder clients for development
   - Maintains plugin functionality in all environments
   - Comprehensive logging for troubleshooting

### Smithy Build Configuration

The plugin generates authentic `smithy-build.json` configurations that:
- Use `awsSmithyKotlinIncludeOperations` transformer for operation filtering
- Apply `awsSmithyKotlinRemoveDeprecatedShapes` for cleanup
- Configure `kotlin-codegen` plugin with proper service shapes and packages
- Support all AWS service protocols (JSON, XML, Query, REST)

## Quick Start

### 1. Apply the Plugin

```kotlin
plugins {
    id("aws.sdk.kotlin.custom-sdk-build") version "1.0.0-SNAPSHOT"
}
```

### 2. Configure Your Custom SDK

```kotlin
awsCustomSdk {
    region = "us-west-2"
    packageName = "com.mycompany.aws.custom"
    
    services {
        lambda {
            operations(
                LambdaOperations.CreateFunction,
                LambdaOperations.Invoke,
                LambdaOperations.DeleteFunction
            )
        }
        
        s3 {
            operations(
                S3Operations.GetObject,
                S3Operations.PutObject,
                S3Operations.DeleteObject
            )
        }
        
        dynamodb {
            operations(
                DynamoDbOperations.GetItem,
                DynamoDbOperations.PutItem,
                DynamoDbOperations.Query
            )
        }
    }
}
```

### 3. Generate Your Custom SDK

```bash
./gradlew generateAwsCustomClients
```

## Type-Safe Operation Constants

The plugin automatically generates type-safe operation constants for all AWS services:

```kotlin
// Instead of error-prone strings:
operations("CreateFunction", "InvokeFunction") // ❌ "InvokeFunction" is wrong!

// Use type-safe constants:
operations(
    LambdaOperations.CreateFunction,
    LambdaOperations.Invoke  // ✅ Correct operation name
)
```

### Available Constants

- `LambdaOperations` - 68 Lambda operations
- `S3Operations` - 99 S3 operations  
- `DynamoDbOperations` - 57 DynamoDB operations
- `ApiGatewayOperations` - 124 API Gateway operations
- And more...

## DSL Configuration Options

### Basic Service Configuration

```kotlin
awsCustomSdk {
    service("lambda") {
        operations(
            LambdaOperations.CreateFunction,
            LambdaOperations.Invoke
        )
    }
}
```

### Multiple Services with Type-Safe DSL

```kotlin
awsCustomSdk {
    services {
        lambda {
            operations(LambdaOperations.CreateFunction, LambdaOperations.Invoke)
        }
        
        s3 {
            operations(S3Operations.GetObject, S3Operations.PutObject)
        }
        
        dynamodb {
            operations(DynamoDbOperations.GetItem, DynamoDbOperations.PutItem)
        }
    }
}
```

### Advanced Selection Methods

```kotlin
awsCustomSdk {
    services {
        lambda {
            // Add all available operations
            allOperations()
        }
        
        s3 {
            // Add operations matching a pattern
            operationsMatching("Get.*|Put.*")
        }
        
        dynamodb {
            // Mix different selection methods
            operations(DynamoDbOperations.GetItem, DynamoDbOperations.PutItem)
            operationsMatching("List.*")
        }
    }
}
```

### Backward Compatibility

String literals are still supported for backward compatibility:

```kotlin
awsCustomSdk {
    service("lambda") {
        operations("CreateFunction", "Invoke", "DeleteFunction")
    }
}
```

## Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `region` | String | `"us-east-1"` | AWS region for generated clients |
| `outputDirectory` | String | `"build/generated/aws-custom-sdk"` | Output directory for generated code |
| `packageName` | String | `"aws.sdk.kotlin.custom"` | Package name for generated clients |
| `strictValidation` | Boolean | `true` | Enable strict validation of operation names |

## Validation and Error Handling

## Real Smithy Build Integration

### Requirements

For real client generation (not just placeholders), ensure:

1. **Smithy Models**: AWS service models available in one of:
   - `{project.rootDir}/codegen/sdk/aws-models/{service}.json` (AWS SDK structure)
   - `{project.dir}/models/{service}.json` (local models)
   - `{project.rootDir}/models/{service}.json` (project root models)

2. **Smithy CLI**: Available through:
   - Project dependencies (recommended)
   - Gradle cache (automatic discovery)
   - Runtime classpath

### Adding Smithy CLI Dependency

```kotlin
dependencies {
    implementation("software.amazon.smithy:smithy-cli:1.60.2")
}
```

### Build Process

The plugin follows this execution flow:

1. **Configuration Generation**: Creates `smithy-build.json` with:
   - Service projections for each configured service
   - Operation filtering using `awsSmithyKotlinIncludeOperations`
   - Kotlin codegen plugin configuration
   - Proper package and service shape mapping

2. **Build Execution**: Attempts in order:
   - Gradle Smithy build plugin (if available)
   - Direct Smithy CLI execution
   - Fallback to placeholder generation

3. **Output Processing**: Generates:
   - Service clients with only selected operations
   - Usage examples and documentation
   - Dependency configuration
   - README with implementation details

### Troubleshooting

Enable debug logging to see the build process:

```kotlin
awsCustomSdk {
    // Plugin will log detailed information about:
    // - Model discovery attempts
    // - Smithy CLI location
    // - Build execution steps
    // - Fallback reasons
}
```

Common issues:
- **"Service model not found"**: Add models to expected locations
- **"Smithy CLI JAR not found"**: Add Smithy CLI dependency
- **"Real Smithy build failed"**: Check logs for specific errors

### Automatic Validation

The plugin automatically validates operation names against known AWS service operations:

```kotlin
awsCustomSdk {
    service("lambda") {
        operations(
            LambdaOperations.CreateFunction,  // ✅ Valid
            "InvalidOperation"                // ❌ Invalid - will show warning
        )
    }
}
```

### Validation Output

```
AWS Custom SDK: Invalid operations for lambda: InvalidOperation
Available operations for lambda: AddLayerVersionPermission, AddPermission, CreateAlias, CreateCodeSigningConfig, CreateEventSourceMapping, CreateFunction, ...
```

### Disable Strict Validation

```kotlin
awsCustomSdk {
    strictValidation = false  // Allow any operation names
    
    service("lambda") {
        operations("CustomOperation")  // Won't show warnings
    }
}
```

## Advanced Usage Examples

### Serverless Application

```kotlin
awsCustomSdk {
    services {
        lambda {
            operations(
                LambdaOperations.CreateFunction,
                LambdaOperations.Invoke,
                LambdaOperations.UpdateFunctionCode,
                LambdaOperations.DeleteFunction
            )
        }
        
        s3 {
            operationsMatching("Get.*|Put.*|Delete.*")
        }
        
        dynamodb {
            operations(
                DynamoDbOperations.GetItem,
                DynamoDbOperations.PutItem,
                DynamoDbOperations.UpdateItem,
                DynamoDbOperations.DeleteItem,
                DynamoDbOperations.Query,
                DynamoDbOperations.Scan
            )
        }
    }
}
```

### Data Processing Pipeline

```kotlin
awsCustomSdk {
    services {
        s3 {
            operations(
                S3Operations.GetObject,
                S3Operations.PutObject,
                S3Operations.ListObjects,
                S3Operations.CopyObject
            )
        }
        
        lambda {
            operations(
                LambdaOperations.Invoke,
                LambdaOperations.InvokeAsync
            )
        }
    }
}
```

### API Gateway Integration

```kotlin
awsCustomSdk {
    services {
        apigateway {
            operationsMatching("Get.*|Create.*|Update.*|Delete.*")
        }
        
        lambda {
            operations(
                LambdaOperations.CreateFunction,
                LambdaOperations.AddPermission,
                LambdaOperations.UpdateFunctionConfiguration
            )
        }
    }
}
```

## Generated Constants Structure

Each service has its own constants object:

```kotlin
object LambdaOperations {
    const val CreateFunction = "CreateFunction"
    const val Invoke = "Invoke"
    const val DeleteFunction = "DeleteFunction"
    // ... 65 more operations
}

object S3Operations {
    const val GetObject = "GetObject"
    const val PutObject = "PutObject"
    const val DeleteObject = "DeleteObject"
    // ... 96 more operations
}
```

## Tasks

| Task | Description |
|------|-------------|
| `generateAwsCustomClients` | Generate custom AWS service clients based on configuration |

## Requirements

- Gradle 7.0+
- Kotlin 1.8+
- Java 11+

## Development

### Building the Plugin

```bash
./gradlew :aws-custom-sdk-build-plugin:build
```

### Running Tests

```bash
./gradlew :aws-custom-sdk-build-plugin:test
```

### Generating Operation Constants

Operation constants are automatically generated during the AWS SDK build process. To regenerate them:

```bash
AWS_SDK_KOTLIN_GENERATE_DSL_CONSTANTS=true ./gradlew :codegen:sdk:bootstrap
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
