package aws.sdk.kotlin.example

import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import aws.sdk.kotlin.services.cloudwatchlogs.model.GetLogEventsRequest
import aws.sdk.kotlin.services.cloudwatchlogs.model.OutputLogEvent
import kotlinx.coroutines.runBlocking

/**
 * This program reads log events from a specified AWS CloudWatch Logs stream.
 *
 * Usage:
 * The program expects three command-line arguments:
 * 1. `region` - The AWS region where the CloudWatch Logs are located.
 * 2. `logGroupName` - The name of the CloudWatch Logs group.
 * 3. `logStreamName` - The name of the CloudWatch Logs stream.
 *
 * The program performs the following steps:
 * 1. Validates the input arguments.
 * 2. Creates a `CloudWatchLogsClient` for the specified region.
 * 3. Uses the `CloudWatchLogsReader` to fetch and print log events from the specified log stream.
 * 4. Handles any exceptions that occur during the process and prints error messages.
 * 5. Closes the `CloudWatchLogsClient` after processing.
 *
 * Example:
 * tasks.named<org.graalvm.buildtools.gradle.tasks.NativeRunTask>("nativeRun") {
 * this.runtimeArgs = listOf("us-southeast-1", "my-log-group", "my-log-stream")
 * }
 *
 * Ensure that the AWS credentials and necessary permissions are configured properly for accessing CloudWatch Logs. */
fun main(args: Array<String>) = runBlocking {

    val usage = """
        Usage: Required <region> <logGroupName> <logStreamName>
    """.trimIndent()
    if (args.size != 3) {
        throw IllegalArgumentException(usage)
    }

    val region = args[0]
    val logGroupName = args[1]
    val logStreamName = args[2]

    val client = CloudWatchLogsClient { this.region = region }

    try {
        // Print log events for the specified log stream
        val logEvents = client.getLogEvents(logGroupName, logStreamName)
        logEvents.forEach { logEvent ->
            println("Log Event: ${logEvent.message}")
        }
    } catch (ex: Exception) {
        println("Error: ${ex.message}")
    } finally {
        client.close()
    }
}

suspend fun CloudWatchLogsClient.getLogEvents(logGroupName: String, logStreamName: String): List<OutputLogEvent> {
    val request = GetLogEventsRequest {
        this.logGroupName = logGroupName
        this.logStreamName = logStreamName
    }

    val response = this.getLogEvents(request)
    return response.events ?: emptyList()
}