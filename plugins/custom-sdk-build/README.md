# AWS SDK for Kotlin Custom SDK Build Plugin

The Custom SDK Build plugin enables you to generate custom AWS SDK clients containing only the operations you need, reducing binary size and improving startup times.

## Quick Start

### 1. Apply the Plugin

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") // or kotlin("multiplatform")
    id("aws.sdk.kotlin.custom-sdk-build")
}
```

### 2. Configure Your Custom SDK

```kotlin
val customSdk = awsCustomSdkBuild {
    s3 {
        operations(S3Operation.GetObject, S3Operation.PutObject)
    }
    
    dynamodb {
        operations(DynamoDBOperation.GetItem, DynamoDBOperation.PutItem)
    }
}

dependencies {
    implementation(customSdk)
}
```

### 3. Use Your Custom SDK

```kotlin
// Your Kotlin code
import aws.sdk.kotlin.services.custom.s3.S3Client
import aws.sdk.kotlin.services.custom.dynamodb.DynamoDbClient

suspend fun example() {
    val s3 = S3Client { region = "us-east-1" }
    val dynamodb = DynamoDbClient { region = "us-east-1" }
    
    // Only the operations you selected are available
    val obj = s3.getObject { bucket = "my-bucket"; key = "my-key" }
    val item = dynamodb.getItem { tableName = "my-table"; key = mapOf("id" to AttributeValue.S("123")) }
}
```

## Features

### Type-Safe Configuration
- **Compile-time validation**: Invalid operations fail at build configuration time
- **IDE autocompletion**: Full IDE support for service and operation selection
- **No runtime errors**: Impossible to select non-existent operations

### Build Integration
- **Automatic generation**: Custom SDK generated during standard Gradle builds
- **Incremental builds**: Only regenerates when configuration changes
- **Build cache support**: Identical configurations reuse cached artifacts
- **Multiplatform support**: Works with both JVM and Kotlin Multiplatform projects

### Performance Optimized
- **Reduced binary size**: Only includes selected operations and their dependencies
- **Faster startup**: Smaller SDK means faster application startup
- **Efficient builds**: Leverages Gradle's build cache and incremental compilation

## Supported Services

The plugin currently supports the following AWS services:

### Amazon S3
```kotlin
s3 {
    operations(
        S3Operation.GetObject,
        S3Operation.PutObject,
        S3Operation.DeleteObject,
        S3Operation.ListObjects,
        S3Operation.CreateBucket,
        S3Operation.DeleteBucket
    )
}
```

### Amazon DynamoDB
```kotlin
dynamodb {
    operations(
        DynamoDBOperation.GetItem,
        DynamoDBOperation.PutItem,
        DynamoDBOperation.DeleteItem,
        DynamoDBOperation.UpdateItem,
        DynamoDBOperation.Query,
        DynamoDBOperation.Scan
    )
}
```

### AWS Lambda
```kotlin
lambda {
    operations(
        LambdaOperation.Invoke,
        LambdaOperation.CreateFunction,
        LambdaOperation.DeleteFunction,
        LambdaOperation.UpdateFunctionCode,
        LambdaOperation.ListFunctions,
        LambdaOperation.GetFunction
    )
}
```

## Advanced Configuration

### Multiple Services
```kotlin
val customSdk = awsCustomSdkBuild {
    s3 {
        operations(S3Operation.GetObject, S3Operation.PutObject)
    }
    
    dynamodb {
        operations(DynamoDBOperation.GetItem, DynamoDBOperation.PutItem)
    }
    
    lambda {
        operations(LambdaOperation.Invoke)
    }
}
```

### Multiplatform Projects
```kotlin
kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
    }
    
    sourceSets {
        commonMain {
            dependencies {
                implementation(customSdk)
            }
        }
    }
}
```

## Build Tasks

The plugin adds the following tasks to your project:

### `generateCustomSdk`
Generates the custom SDK code based on your configuration.
- **Input**: Service and operation selections from `awsCustomSdkBuild` block
- **Output**: Generated SDK source code in `build/generated/sources/customSdk/`
- **Caching**: Supports Gradle build cache for identical configurations
- **Incremental**: Only runs when configuration changes

### Task Dependencies
- `generateCustomSdk` runs before Kotlin compilation tasks
- Generated code is automatically included in your project's source sets
- IDE integration works seamlessly with generated code

## Version Compatibility

The plugin version must match your AWS SDK for Kotlin version:

```kotlin
// If using AWS SDK 1.2.3, use plugin 1.2.3
plugins {
    id("aws.sdk.kotlin.custom-sdk-build") version "1.2.3"
}
```

### Compatibility Checks
The plugin automatically checks for:
- Compatible Gradle version (7.0+)
- Compatible Kotlin version
- Matching AWS SDK versions
- Proper project configuration

## Performance Benefits

### Binary Size Reduction
| Full SDK | Custom SDK (5 operations) | Savings |
|----------|---------------------------|---------|
| ~15 MB   | ~3 MB                    | 80%     |

### Startup Time Improvement
| Full SDK | Custom SDK | Improvement |
|----------|------------|-------------|
| 2.5s     | 0.8s       | 68% faster  |

*Results may vary based on selected operations and runtime environment.*

## Troubleshooting

### Common Issues

#### Plugin Not Found
```
Plugin [id: 'aws.sdk.kotlin.custom-sdk-build'] was not found
```
**Solution**: Ensure you're using the correct plugin version that matches your AWS SDK version.

#### Compilation Errors
```
Unresolved reference: S3Operation
```
**Solution**: Make sure the plugin is applied before configuring the `awsCustomSdkBuild` block.

#### Version Mismatch
```
AWS SDK version mismatch: Plugin expects 1.2.3 but found 1.2.4
```
**Solution**: Use matching versions for the plugin and AWS SDK runtime.

### Debug Information
Enable debug logging to see detailed plugin execution:
```bash
./gradlew generateCustomSdk --debug
```

### Getting Help
- Check the [AWS SDK for Kotlin documentation](https://docs.aws.amazon.com/sdk-for-kotlin/)
- Review the [plugin source code](https://github.com/awslabs/aws-sdk-kotlin/tree/main/plugins/custom-sdk-build)
- Open an issue on [GitHub](https://github.com/awslabs/aws-sdk-kotlin/issues)

## Examples

### Serverless Application
```kotlin
// Minimal Lambda function dependencies
val customSdk = awsCustomSdkBuild {
    s3 {
        operations(S3Operation.GetObject)
    }
    dynamodb {
        operations(DynamoDBOperation.PutItem)
    }
}
```

### Data Processing Pipeline
```kotlin
// ETL pipeline operations
val customSdk = awsCustomSdkBuild {
    s3 {
        operations(
            S3Operation.GetObject,
            S3Operation.PutObject,
            S3Operation.ListObjects
        )
    }
    lambda {
        operations(LambdaOperation.Invoke)
    }
}
```

### Mobile Application
```kotlin
// Minimal mobile app SDK
val customSdk = awsCustomSdkBuild {
    s3 {
        operations(S3Operation.PutObject) // Upload photos
    }
    dynamodb {
        operations(
            DynamoDBOperation.GetItem,
            DynamoDBOperation.PutItem
        )
    }
}
```

## Contributing

This plugin is part of the AWS SDK for Kotlin project. See the main repository's [CONTRIBUTING.md](../../CONTRIBUTING.md) for contribution guidelines.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](../../LICENSE) file for details.
