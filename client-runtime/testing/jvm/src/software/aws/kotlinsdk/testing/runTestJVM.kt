/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.aws.kotlinsdk.testing

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun <T> runSuspendTest(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    runBlocking { block(this) }
