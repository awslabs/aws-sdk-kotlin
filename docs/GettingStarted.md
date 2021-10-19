# Alpha Release Quickstart

Alpha releases of the AWS SDK for Kotlin are published to Maven Central with the `-alpha` qualifier. 
**NOTE: Alpha releases are not meant for production workloads**.
Consult the [stability guide](stability.md) for more information on SDK stability and maintenance.

1. Add the repository to your Gradle or Maven configuration

    **Gradle Users**

    ```kt
    # file: my-project/build.gradle.kts

    repositories {
        mavenCentral()
    }
    ```


2. Add services to your project

    ```kt
    # file: my-project/build.gradle.kts

    val awsKotlinSdkVersion = "0.7.0-alpha"
    // OR put it in gradle.properties
    // val awsKotlinSdkVersion: String by project

    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
        
        // The following line adds a dependency on the dynamodb client.
        implementation("aws.sdk.kotlin:dynamodb:$awsKotlinSdkVersion")
    }

3. Make a request
   
    Example for `DynamoDB`:

    ```kotlin
    import kotlinx.coroutines.runBlocking
    import aws.sdk.kotlin.services.dynamodb.DynamoDbClient

    fun main() = runBlocking {
        val client = DynamoDbClient { region = "us-east-2" }
        val response = client.listTables { limit = 10 }

        println("Current DynamoDB tables: ")
        response.tableNames?.forEach { println(it) }

        client.close()
    }
    ```

    Operations that return streaming responses are slightly different. The response must be handled entirely within a
    block passed to the API call. Example for `S3`:
   
    ```kotlin
    import kotlinx.coroutines.runBlocking
    import aws.sdk.kotlin.services.dynamodb.S3Client

    fun main() = runBlocking {
        val client = S3Client { region = "us-east-2" }
        val request = GetObjectRequest { key = "path/to/object"; bucket = "the-bucket" }
   
        client.getObject(request) { response ->
            val outputFile = File("/path/to/the/file")
            response.body?.writeToFile(outputFile).also { size ->
                println("Downloaded $outputFile ($size bytes) from S3")
            }
        }
   
        client.close()
    }
    ```

## Additional Resources

* [Additional examples](https://github.com/awslabs/aws-sdk-kotlin/tree/main/examples)
* [Giving feedback and contributing](https://github.com/awslabs/aws-sdk-kotlin#feedback)
* [Debugging](debugging.md)
* [Android support](targets.md#android)
