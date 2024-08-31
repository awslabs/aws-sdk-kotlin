package aws.sdk.kotlin.example

import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import aws.sdk.kotlin.services.cloudwatchlogs.model.*

class CloudWatchLogsReader(private val client: CloudWatchLogsClient) {

    suspend fun getLogEvents(logGroupName: String, logStreamName: String): List<OutputLogEvent> {
        val request = GetLogEventsRequest {
            this.logGroupName = logGroupName
            this.logStreamName = logStreamName
        }

        val response = client.getLogEvents(request)
        return response.events ?: emptyList()
    }

    suspend fun listLogGroups(): List<LogGroup> {
        val request = DescribeLogGroupsRequest {}
        val response = client.describeLogGroups(request)
        return response.logGroups ?: emptyList()
    }

    suspend fun listLogStreams(logGroupName: String): List<LogStream> {
        val request = DescribeLogStreamsRequest {
            this.logGroupName = logGroupName
        }
        val response = client.describeLogStreams(request)
        return response.logStreams ?: emptyList()
    }
}