/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.testing

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import aws.smithy.kotlin.runtime.testing.runSuspendTest as runTest

// TODO - migrate unit tests to just use runSuspendTest from smithy-kotlin, for now re-export it to limit # changes
/**
 * MPP compatible runBlocking to run suspend tests in common modules
 */
public fun <T> runSuspendTest(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T = runTest(context, block)
