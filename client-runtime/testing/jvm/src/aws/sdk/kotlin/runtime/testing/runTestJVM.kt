/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

public actual fun <T> runSuspendTest(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    runBlocking { block(this) }
