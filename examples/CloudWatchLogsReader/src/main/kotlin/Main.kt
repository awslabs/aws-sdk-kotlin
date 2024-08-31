package aws.sdk.kotlin.example

import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import kotlinx.coroutines.runBlocking

/**
 * Ensure the following environment variables are set:
 * - REGION
 * - LOG_GROUP_NAME
 * - LOG_STREAM_NAME
 */
fun main(args: Array<String>) = runBlocking {
    if (args.size != 3) {
        throw IllegalArgumentException("Usage: Required <region> <logGroupName> <logStreamName>")
    }

    val region = args[0]
    val logGroupName = args[1]
    val logStreamName = args[2]

    val client = CloudWatchLogsClient { this.region = region }
    val logReader = CloudWatchLogsReader(client)

    try {
        println("Log Group: $logGroupName")
        println("  Log Stream: $logStreamName")

        // Print log events for the specified log stream
        val logEvents = logReader.getLogEvents(logGroupName, logStreamName)
        logEvents.forEach { logEvent ->
            println("    Log Event: ${logEvent.message}")
        }
    } catch (ex: Exception) {
        println("Error: ${ex.message}")
    } finally {
        client.close()
    }
}