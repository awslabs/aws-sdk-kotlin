/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.smithy.kotlin.runtime.logging.Logger
import io.ktor.server.engine.*
import java.net.*
import java.util.concurrent.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

public abstract class TestWithLocalServer {
    protected val serverPort: Int = ServerSocket(0).use { it.localPort }
    protected val testHost: String = "localhost:$serverPort"
    protected val testUrl: String = "http://$testHost"

    public abstract val server: ApplicationEngine

    private val logger = Logger.getLogger<TestWithLocalServer>()

    @BeforeTest
    public fun startServer() {
        var attempt = 0

        do {
            attempt++
            try {
                server.start()
                logger.info { "test server listening on: $testHost" }
                break
            } catch (cause: Throwable) {
                if (attempt >= 10) throw cause
                Thread.sleep(250L * attempt)
            }
        } while (true)

        ensureServerRunning()
    }

    @AfterTest
    public fun stopServer() {
        server.stop(0, 0, TimeUnit.SECONDS)
        logger.info { "test server stopped" }
    }

    private fun ensureServerRunning() {
        do {
            try {
                Socket("localhost", serverPort).close()
                break
            } catch (_: Throwable) {
                Thread.sleep(100)
            }
        } while (true)
    }
}
