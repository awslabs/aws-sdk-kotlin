/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http

import aws.smithy.kotlin.runtime.util.OsFamily
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsUserAgentMetadataTest {

    @Test
    fun testUserAgent() {
        val ua = AwsUserAgentMetadata.fromEnvironment(ApiMetadata("Test Service", "1.2.3"))
        assertEquals("aws-sdk-kotlin/1.2.3", ua.userAgent)
    }

    @Test
    fun testXAmzUserAgent() {
        val apiMeta = ApiMetadata("Test Service", "1.2.3")
        val sdkMeta = SdkMetadata("kotlin", apiMeta.version)
        val osMetadata = OsMetadata(OsFamily.Linux, "ubuntu-20.04")
        val langMeta = LanguageMetadata("1.4.31", mapOf("jvmVersion" to "1.11"))
        val ua = AwsUserAgentMetadata(sdkMeta, apiMeta, osMetadata, langMeta)
        val expected = "aws-sdk-kotlin/1.2.3 api/test-service/1.2.3 os/linux/ubuntu-20.04 lang/kotlin/1.4.31 md/jvmVersion/1.11"
        assertEquals(expected, ua.xAmzUserAgent)
    }
}
