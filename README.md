# AWS SDK for Kotlin

The **AWS SDK for Kotlin** is a wrapper around the [AWS SDK for Java v2](https://raw.githubusercontent.com/aws/aws-sdk-java-v2) exposing model objects as Kotlin a [`data class`](https://kotlinlang.org/docs/reference/data-classes.html) allowing more a more native Kotlin experience.

Allows making calls to AWS services in a more native Kotlin fashion:

```kotlin
val client = S3Client { region(Region.US_WEST_1) }
client.createBucket {
    bucket = "some-bucket-name"
    createBucketConfiguration {
        locationConstraint = US_WEST_1
    }
}
```

Prettier statically typed DSL - useful for creating heavily hierachial request objects:

```kotlin
SendEmailRequest {
    destination {
        toAddresses = listOf("someone@example.com")
    }
    replyToAddresses = listOf("someone_else@example.com")
    message {
        subject {
            data = "The Email Subject"
        }
        body {
            text {
                data = "The email body"
                charset = "UTF-8"
            }
        }
    }
}
```

This is distributed as a build plugin that ties into your build system (supported build systems: `maven` & `gradle`) and generates the Kotlin wrappers for the services that you require in your project.

### Gradle Plugin

If you have a gradle project you can add the following dependency to your `buildscript` to make the plugin available.

```groovy
buildscript {
    dependencies {
        classpath(group: "software.amazon.awssdk.kotlin", name: "codegen-gradle-plugin", version: "1.0-SNAPSHOT", changing: true)
    }
}
```

Then configure the task passing the services that you want to generate Kotlin wrappers for *(note: the AWS SDK for Java v2 version of that service client must exist in your project's compile-time dependencies)*:

```groovy
dependencies {
    compile "software.amazon.awssdk:s3:2.0.0-preview-7"
}

awsKotlin {
    services = [ "s3" ]
}
```

This is automatically bound as a dependency for the `compileKotlin` task, so running `gradle compileKotlin` should invoke the code generator and then compile all Kotlin code. You'll notice that a `build/generated-src/ktSdk` directory is created with the models for the services you configured. Full set of options available to configure (along with their defaults) below:

```groovy
awsKotlin {
    // The services to generate wrappers for - can be in the following forms:
    // - reference to an actual Java SDK client class (e.g. S3Client)
    // - fully-qualified name of a Java SDK Client class (e.g. "software.amazon.awssdk.services.s3.S3Client")
    // - the artifactId (and optionally groupId) of a Java SDK service module (e.g. "software.amazon.awssdk:s3" or simply "s3")  
    services = [ ]
    
    // This is where the generated source files will be put; this path is automatically added to the "main" sourceSet
    outputDirectory = "${project.buildDir}/generated-src/ktSdk"
    
    // Whether or not to place model/transform classes in their own .kt files or combine into a single file
    minimizeFiles = false
    
    // Base package name to generate wrappers into
    targetBasePackage = "software.amazon.awssdk.kotlin"
}
```

### Maven Plugin

To use with Maven add the following to your `<build>` configuration:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>software.amazon.awssdk.kotlin</groupId>
            <artifactId>codegen-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                    <configuration>
                        <services>s3</services>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Wrapper classes are generated into 
Similar to gradle, there are several configurable options (with their defaults shown below):

```xml
<configuration>
    <!-- A comma separated list of services can be in the forms:
     - fully-qualified name of a Java SDK Client class (e.g. software.amazon.awssdk.services.s3.S3Client)
     - the artifactId (and optionally groupId) of a Java SDK service module (e.g. "software.amazon.awssdk:s3" or simply "s3")
     -->   
    <services/>
    
    <!-- This is where generated source files will be put. You will need to add this as a `<sourceDir>` 
    to your kotlin-maven-plugin configuration. -->
    <outputDirectory>${project.build.directory}/generated-sources/ktSdk</outputDirectory>
    
    <!-- Whether or not to place model/transform classes in their own .kt files or combine into a single file -->
    <minimizeFiles>false</minimizeFiles>
    
    <!-- Base package name to generate wrappers into -->
    <targetBasePackage>software.amazon.awssdk.kotlin</targetBasePackage>
</configuration>
```

## License

This library is licensed under the Apache 2.0 License. 
