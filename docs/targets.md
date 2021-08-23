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
* Exclude `xmlpull` module

Example config fragments:

```kotlin
// build.gradle.kts

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

// get around a build issue with xmlpull as a dependency
configurations.all {
    exclude(group = "xmlpull", module = "xmlpull")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")
}
```
