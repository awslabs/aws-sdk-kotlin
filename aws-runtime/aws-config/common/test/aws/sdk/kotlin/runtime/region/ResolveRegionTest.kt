/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.client.ExecutionContext
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveRegionTest {

    @Test
    fun testResolutionOrder() = runSuspendTest {
        // from context
        val config = object : RegionConfig {
            override val region: String = "us-west-2"
        }

        // context has highest priority
        val actual = resolveRegionForOperation(ctx = ExecutionContext().apply { set(AwsClientOption.Region, "us-east-1") }, config)
        assertEquals("us-east-1", actual)

        // from config is next
        val actual2 = resolveRegionForOperation(ExecutionContext(), config)
        assertEquals("us-west-2", actual2)
    }
}
