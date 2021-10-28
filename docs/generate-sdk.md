# Generate an AWS SDK

This page describes how to generate and build an AWS SDK. Generating an SDK from source may be useful in testing fixes,
learning how AWS SDKs work internally, or just experimentation.

### NOTE

This process is not necessary to use an AWS SDK, as one can simply depend on the artifacts available in 
[Maven Central](https://search.maven.org/search?q=aws.sdk.kotlin).  See the [usage doc](use-aws-sdk.md) for details.

## Prerequisites
* Git
* Java JDK 1.8+

## Generate an SDK

In this example we'll build the AWS DynamoDB SDK from the `0.8.0-alpha` release.

### Clone this repo using a release tag
```
git clone --branch v0.8.0-alpha https://github.com/awslabs/aws-sdk-kotlin.git
cd aws-sdk-kotlin
```

### Generate the SDK source

Before building the SDK we will change its version to prevent having the same version as provided by Maven Central:
```
sed -i 's/sdkVersion=0.8.0-alpha/sdkVersion=0.8.0-localtest/' gradle.properties
```

Now we can generate the DynamoDB SDK:
```
./gradlew -Paws.services=+dynamodb :codegen:sdk:bootstrap
```
Notice we specify the name of the service as `dynamodb`.  The names for all AWS services can be found in 
`codegen/sdk/aws-models`.  So for example the name used for AWS CloudFormation's model `cloudformation.2010-05-15.json` 
would be `cloudformation`.  The date and json extension are removed.

### Compile and test the generated SDK
```
./gradlew test jar
```
Once this completes a compiled SDK for the AWS DynamoDB service has been generated.

```
ls services/dynamodb/build/libs      
dynamodb-0.8.0-localtest.jar
```

To use this compiled SDK from another program locally, the local Maven repository can be used.  The following command 
will publish artifacts from `aws-sdk-kotlin` to the local maven repository, including the DynamoDB service and supporting
runtime libraries:

```
./gradlew publishToMavenLocal
```

## Using a locally-built SDK

In order to use an SDK published to the local maven repository, the `mavenLocal()` repository must be added to the program's
build file:

```
repositories {
    mavenLocal()
}
```

And then simply add the dependency:

```
dependencies {
    implementation("aws.sdk.kotlin:dynamodb:0.8.0-localtest")
}
```

## Summary

This page covers building an AWS SDK using the `aws-sdk-kotlin` SDK generator. If you experience problems please [file 
an issue](https://github.com/awslabs/aws-sdk-kotlin/issues). If you have questions or ideas about how we can improve our 
SDKs, please join [our discussions](https://github.com/awslabs/aws-sdk-kotlin/discussions).