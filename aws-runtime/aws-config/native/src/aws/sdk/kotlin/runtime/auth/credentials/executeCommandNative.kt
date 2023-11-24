/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.PlatformProvider

internal actual suspend fun executeCommand(
    command: String,
    platformProvider: PlatformProvider,
    maxOutputLengthBytes: Long,
    timeoutMillis: Long,
    clock: Clock,
): Pair<Int, String> {
    TODO("Not yet implemented")
}
