package aws.sdk.kotlin.example

import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import kotlinx.coroutines.runBlocking

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
    val logReader = CloudWatchLogsReader(client)

    try {
        // Print log events for the specified log stream
        val logEvents = logReader.getLogEvents(logGroupName, logStreamName)
        logEvents.forEach { logEvent ->
            println("Log Event: ${logEvent.message}")
        }
    } catch (ex: Exception) {
        println("Error: ${ex.message}")
    } finally {
        client.close()
    }
}