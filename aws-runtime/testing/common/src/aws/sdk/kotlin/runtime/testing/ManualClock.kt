/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.testing

import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.Instant
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

public class ManualClock(epoch: Instant = Instant.now()) : Clock {
    private var now: Instant = epoch

    @OptIn(ExperimentalTime::class)
    public fun advance(duration: Duration) {
        now = duration.toComponents { seconds, nanoseconds ->
            Instant.fromEpochSeconds(now.epochSeconds + seconds, now.nanosecondsOfSecond + nanoseconds)
        }
    }

    override fun now(): Instant = now
}
