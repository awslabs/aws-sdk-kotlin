# Smithy Kotlin Transforms Research

## Overview
Research into the Smithy Kotlin transform system, specifically the `awsSmithyKotlinIncludeOperations` transform that will be key to our custom SDK plugin.

## Key Findings

### IncludeOperations Transform
- **Location**: `smithy-kotlin/codegen/smithy-kotlin-codegen/src/main/kotlin/software/amazon/smithy/kotlin/codegen/transforms/IncludeOperations.kt`
- **Usage**: Currently used in `aws-sdk-kotlin/aws-runtime/aws-config/build.gradle.kts`
- **Purpose**: Filters Smithy models to include only specified operations and their dependencies

### Transform Configuration Format
Based on `aws-runtime/aws-config/build.gradle.kts`, the transform is configured as JSON:

```kotlin
transforms = listOf(
    """
    {
        "name": "awsSmithyKotlinIncludeOperations",
        "args": {
            "operations": [
                "com.amazonaws.sts#AssumeRole",
                "com.amazonaws.sts#AssumeRoleWithWebIdentity"
            ]
        }
    }
    """,
)
```

### Transform Integration
- Transforms are applied during Smithy projection creation
- Can be configured per service/projection
- Integrated into the existing `SmithyProjection` DSL
- Operations are specified by their full Smithy shape IDs

## Implications for Custom SDK Plugin

### Transform Usage Pattern
1. The plugin will need to apply the `awsSmithyKotlinIncludeOperations` transform
2. Transform configuration will specify which operations to include using full shape IDs
3. Transform will automatically handle dependency resolution (data types, etc.)
4. Plugin needs to map user's DSL selections to Smithy shape IDs

### Integration Points
- Plugin needs to create Smithy projections similar to current SDK build
- Transform configuration will be driven by user's DSL selections
- Generated code will only contain selected operations and required types
- Need to resolve operation constants to their full Smithy shape IDs

## Next Steps
- Research how to map operation constants to Smithy shape IDs
- Investigate Smithy projection creation patterns for plugins
- Examine source set integration patterns from aws-config example
