# Beta Release Quickstart

Beta releases of the AWS Kotlin SDK are published as a complete maven local repository with all associated dependencies.


1. Download the [latest release](https://github.com/awslabs/aws-sdk-kotlin/releases) from Github

2. Unzip the repository somewhere on your local machine

```sh
> unzip aws-sdk-kotlin-0.1.0-M0.zip
```

There should be a folder named `aws-sdk-kotlin-repo`

3. Add the local repository to your Gradle or Maven configuration

#### Gradle Users

```kt
# file: my-project/build.gradle.kts


repositories {
    maven {
        name = "kotlinSdkLocal"
        url = uri("/path/to/aws-sdk-kotlin-repo/m2")
    }
    mavenCentral()
}
```

#### Maven Users
```xml
<project>
...
<repositories>
    <repository>
        <id>kotlinSdkLocal</id>
        <name>Beta AWS Kotlin SDK Repo</name>
        <url>/local/path/to/aws-sdk-kotlin-repo/m2</url>
    </repository>
</repositories>
...
</project>

```


4. Add services to your project

Services available for testing: cognitoidentityprovider, kms, lambda, polly, secretsmanager, translate

```kt

val awsKotlinSdkVersion = "0.1.0-M0"
// OR put it in gradle.properties
// val awsKotlinSdkVersion by project

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    
    // The following line adds a dependency on the dynamodb client.
    // Additional services available in the M0 release:
    // cognitoidentityprovider, kms, lambda, polly, secretsmanager, translate
    implementation("aws.sdk.kotlin:dynamodb:$awsKotlinSdkVersion")
}
```


5. Checkout [the `examples` directory](../examples)
   
  Or you can simply begin working with the SDK by creating a service client in your own Kotlin code.  Example for `DynamoDB`:

```kotlin
val client = DynamodbClient { region = "us-east-2" }
...
```


## Giving Feedback

* Slack - Join `[#aws-sdk-kotlin-interest](https://amzn-aws.slack.com/archives/C0182UWTQJJ)` to get help, share feedback, and get updates on SDK development.
* Submit [issues](https://github.com/awslabs/aws-sdk-kotlin/issues)
