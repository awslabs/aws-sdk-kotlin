# Supported Targets

## JVM

The AWS SDK for Kotlin supports running on JDK8+ on the following platforms and architectures:

* Windows: `x86_32`, `x86_64`
* Linux: `x86_32`, `x86_64`, `armv6`, `armv7`, `armv8`
* macOS: `x86_64`

If there is a platform or architecture not supported please submit an issue with details about your use case.

## Android

The AWS SDK for Kotlin supports Android API 24+ (`minSdk = 24`).

NOTE: Later versions of Android may contain security fixes so consider reviewing known vulnerabilities
for the Android versions you choose to support in your application.

Additional requirements:

* Enable [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring)

Example config fragments:

```kotlin
// build.gradle.kts

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    compilerOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")
}
```

## GraalVM

The AWS SDK for Kotlin supports [native image compilation using GraalVM](https://www.graalvm.org/latest/reference-manual/native-image/).

Compiling native images is easily accomplished by applying [the GraalVM build plugin](https://github.com/graalvm/native-build-tools) to your application and running `./gradlew nativeCompile`.
View [their getting started guide](https://graalvm.github.io/native-build-tools/latest/gradle-plugin-quickstart.html) for further details.

NOTE: [log4j2 does not currently support](https://github.com/apache/logging-log4j2/issues/1539) GraalVM native image compilation, so we recommend selecting a different logger implementation such as Logback.
