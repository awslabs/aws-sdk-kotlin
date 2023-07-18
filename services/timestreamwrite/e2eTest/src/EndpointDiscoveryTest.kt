/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.timestreamwrite

import aws.sdk.kotlin.services.timestreamwrite.model.DescribeEndpointsResponse
import aws.sdk.kotlin.services.timestreamwrite.model.ListDatabasesResponse
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.client.operationName
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndpointDiscoveryTest {
    @Test
    fun testEndpointDiscovery() = runBlocking {
        val operationLog = mutableListOf<String>()
        var discoveredHost: String? = null

        val interceptor = object : HttpInterceptor {
            override fun readAfterExecution(
                context: ResponseInterceptorContext<Any, Any, HttpRequest?, HttpResponse?>,
            ) {
                operationLog += context.executionContext.operationName!!

                when (val response = context.response.getOrNull()) {
                    is DescribeEndpointsResponse -> discoveredHost = response.endpoints!!.first().address

                    is ListDatabasesResponse -> {
                        // Make sure every request _except_ DescribeEndpoints uses the discovered endpoint
                        assertNotNull(discoveredHost)
                        assertEquals(discoveredHost, context.protocolRequest!!.url.host.toString())
                    }

                    else -> error("Unexpected response ${context.response}")
                }
            }
        }

        TimestreamWriteClient {
            region = "us-west-2"
            interceptors += interceptor
        }.use { tsw ->
            tsw.listDatabases()

            // Have to discover the endpoint the first time
            assertEquals(listOf("DescribeEndpoints", "ListDatabases"), operationLog)
            operationLog.clear()

            tsw.listDatabases()

            // Don't have to discover the endpoint again because it's cached
            assertEquals(listOf("ListDatabases"), operationLog)
        }
    }
}
