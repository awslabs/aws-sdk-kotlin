/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.testing

import aws.smithy.kotlin.runtime.http.engine.DefaultHttpEngine
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine

/**
 * Printable ASCII characters
 */
val PRINTABLE_CHARS = (32 until 127).map(Int::toChar).joinToString("")

/**
 * Run the [block] with each supported engine
 */
suspend fun withAllEngines(block: suspend (HttpClientEngine) -> Unit) {
    val engines = listOf(
        DefaultHttpEngine(),
        CrtHttpEngine(),
    )

    engines.forEach { engine ->
        engine.use {
            block(engine)
        }
    }
}
