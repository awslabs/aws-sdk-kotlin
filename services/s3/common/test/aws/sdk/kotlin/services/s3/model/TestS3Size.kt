/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.services.s3.model

import kotlin.test.Test
import kotlin.test.assertIs

class TestS3Size {
    @Test
    fun testSizeIsLong() {
        // verify no regressions in `Size` shape reverting to `Integer` (it should be `Long`)
        // see: https://github.com/awslabs/aws-sdk-kotlin/issues/309

        val obj = Object { size = 100 }
        val req = PutObjectRequest {
            bucket = "foo"
            key = "bar"
            contentLength = 100
        }
        assertIs<Long>(obj.size)
        assertIs<Long>(req.contentLength)
    }
}
