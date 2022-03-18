/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.testing

import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.http.engine.SdkHttpEngine
import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine

/**
 * Printable ASCII characters
 */
val PRINTABLE_CHARS = ByteArray(127 - 32) { (it + 32).toByte() }.decodeToString()

/**
 * Run the [block] with each supported engine
 */
suspend fun withAllEngines(block: suspend (HttpClientEngine) -> Unit) {
    val engines = listOf(
        SdkHttpEngine(),
        CrtHttpEngine()
    )

    engines.forEach { engine ->
        engine.use {
            block(engine)
        }
    }
}
