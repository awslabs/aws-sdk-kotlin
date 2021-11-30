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

    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
        
        // The following line adds a dependency on the dynamodb client.
        implementation("aws.sdk.kotlin:dynamodb:0.+")
    }
    ```

3. Configure a service client
   
    Example for `DynamoDB`:

    ```kotlin
    import kotlinx.coroutines.runBlocking
    import aws.sdk.kotlin.services.dynamodb.DynamoDbClient

    fun main() = runBlocking {
        val client = DynamoDbClient { region = "us-east-2" }
        val resp = client.listTables { limit = 10 }

        println("Current DynamoDB tables: ")
        resp.tableNames?.forEach { println(it) }

        client.close()
    }
    ```



## Additional Resources

* [Additional examples](https://github.com/awslabs/aws-sdk-kotlin/tree/main/examples)
* [Giving feedback and contributing](https://github.com/awslabs/aws-sdk-kotlin#feedback)
* [Debugging](debugging.md)
* [Android support](targets.md#android)
