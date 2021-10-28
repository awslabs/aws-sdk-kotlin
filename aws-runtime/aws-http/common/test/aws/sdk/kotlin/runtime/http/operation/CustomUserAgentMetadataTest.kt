/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.operation

import aws.sdk.kotlin.runtime.http.ApiMetadata
import aws.sdk.kotlin.runtime.http.loadAwsUserAgentMetadataFromEnvironment
import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class CustomUserAgentMetadataTest {
    @Test
    fun testCustomMetadata() {
        val provider = TestPlatformProvider()
        val metadata = loadAwsUserAgentMetadataFromEnvironment(provider, ApiMetadata("Test Service", "1.2.3"))
        val customMetadata = CustomUserAgentMetadata()

        customMetadata.add("foo", "bar")
        customMetadata.add("truthy", "true")
        customMetadata.add("falsey", "false")

        val configMetadata = ConfigMetadata("retry-mode", "standard")
        customMetadata.add(configMetadata)

        customMetadata.add(FeatureMetadata("s3-transfer", "1.2.3"))
        customMetadata.add(FeatureMetadata("waiter"))

        val actual = metadata.copy(customMetadata = customMetadata).xAmzUserAgent

        listOf(
            "md/foo/bar",
            "md/truthy",
            "md/falsey/false",
            "cfg/retry-mode/standard",
            "ft/s3-transfer/1.2.3",
            "ft/waiter"
        ).forEach { partial ->
            actual.shouldContain(partial)
        }
    }
}
