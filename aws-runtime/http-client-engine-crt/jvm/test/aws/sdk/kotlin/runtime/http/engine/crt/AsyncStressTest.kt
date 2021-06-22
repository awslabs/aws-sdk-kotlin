/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.Protocol
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineConfig
import aws.smithy.kotlin.runtime.http.readAll
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.http.request.url
import aws.smithy.kotlin.runtime.http.response.complete
import aws.smithy.kotlin.runtime.http.sdkHttpClient
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.async
import kotlinx.coroutines.yield
import kotlin.test.Test

class AsyncStressTest : TestWithLocalServer() {

    override val server = embeddedServer(CIO, serverPort) {
        routing {
            get("/largeResponse") {
                val respSize = 1024 * 32
                val text = "testing"
                call.respondText(text.repeat(respSize / text.length))
            }
        }
    }

    @Test
    fun concurrentRequestTest() = runSuspendTest {
        // https://github.com/awslabs/aws-sdk-kotlin/issues/170
        val client = sdkHttpClient(CrtHttpEngine(HttpClientEngineConfig()))
        val request = HttpRequestBuilder().apply {
            url {
                scheme = Protocol.HTTP
                method = HttpMethod.GET
                host = testHost
                path = "/largeResponse"
            }
        }

        repeat(1_000) {
            async {
                try {

                    val call = client.call(request)
                    yield()
                    // FIXME - this should work WITHOUT having to consume the body
                    call.response.body.readAll()
                    call.complete()
                } catch (ex: Exception) {
                    println("exception on $it: $ex")
                    throw ex
                }
            }
            yield()
        }
    }
}
