# Supported Targets

## Android

The AWS SDK for Kotlin supports Android API 24+ (`minSdk = 24`).

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
