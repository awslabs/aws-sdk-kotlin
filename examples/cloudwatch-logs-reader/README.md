## CloudWatch Logs Reader with GraalVM Native Image

### Overview

This project is a Kotlin-based application that reads log events from AWS CloudWatch Logs. It uses the AWS SDK for
Kotlin and is configured to run as a native image using GraalVM. More information about GraalVM native images can be found in the [official documentation](https://www.graalvm.org/latest/reference-manual/native-image/).

### How to Use

#### Prerequisites

<ul>
<li> JDK 17</li>
<li> GraalVM</li>
<li> Gradle</li>
<li> AWS credentials configured (e.g, using AWS CLI)</li>
</ul>

### What is GraalVM?

GraalVM is a high-performance runtime that provides significant improvements in application performance and efficiency.
It supports multiple languages and execution modes, including:

- **JVM-based languages**: Java, Kotlin, Scala, etc.
- **LLVM-based languages**: C, C++, Rust, etc.
- **Dynamic languages**: JavaScript, Python, Ruby, etc.

GraalVM can compile Kotlin applications into native executables, which improves startup time and reduces memory usage.

### Running the Application with Configured Gradle Task

The `nativeRun` Gradle task is configured to run the application as a native image. Here is a brief explanation of the
configuration:

- **Region**: The AWS region where your CloudWatch Logs are located.
- **Log Group**: The name of the log group you want to read logs from.
- **Log Stream**: The name of the log stream you want to read logs from.
- **The task**: is defined in the `build.gradle.kts` file.

This task uses the GraalVM native image plugin to build and run the application with the specified arguments.

```kotlin
tasks.named<org.graalvm.buildtools.gradle.tasks.NativeRunTask>("nativeRun") {
    this.runtimeArgs = listOf("ap-southeast-1", "my-log-group", "my-log-stream")
}
```

### Reflection Configuration for GraalVM

GraalVM doesn’t support reflection automatically. If your code or any library you use relies on reflection, you’ll need to set it up manually when building native images. This is done by creating a reflection configuration file. The configuration file should be placed in the `src/main/resources/META-INF/native-image` directory.

Here is an example of a reflection configuration file [src/main/resources/META-INF/native-image/reflect-config.json](src/main/resources/META-INF/native-image/aws/sdk/kotlin/example/reflect-config.json):

```json
[
  {
    "name": "com.example.YourClass",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true,
    "allDeclaredFields": true
  }
]
```

In the `build.gradle.kts` file, ensure that the `META-INF/native-image` directory is added to the classpath for reflection configuration:
```kotlin
graalvmNative {
    binaries.all {
        resources.autodetect()
        // Add the META-INF/native-image directory to the classpath for reflection configuration
        configurationFileDirectories.from(file("src/main/resources/META-INF/native-image"))
    }
}
```
This configuration ensures that the necessary reflection metadata is available at runtime for the native image.
For more details, you can refer to the [official GraalVM documentation on reflection configuration](https://www.graalvm.org/latest/reference-manual/native-image/dynamic-features/Reflection/).

