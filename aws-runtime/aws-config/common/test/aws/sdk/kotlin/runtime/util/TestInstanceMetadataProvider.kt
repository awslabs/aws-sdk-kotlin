/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.util

import aws.sdk.kotlin.runtime.config.imds.InstanceMetadataProvider

public class TestInstanceMetadataProvider(private val metadata: Map<String, String>) : InstanceMetadataProvider {
    public companion object { }

    override fun close(): Unit = Unit
    override suspend fun get(path: String): String = metadata[path] ?: throw IllegalArgumentException("$path missing")
}
