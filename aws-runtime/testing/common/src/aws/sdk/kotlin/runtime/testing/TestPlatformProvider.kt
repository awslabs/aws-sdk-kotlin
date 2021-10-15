/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.testing

import aws.smithy.kotlin.runtime.util.Filesystem
import aws.smithy.kotlin.runtime.util.OperatingSystem
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * An implementation of [PlatformProvider] meant for testing
 * @param env Environment variable mappings
 * @param props System property mappings
 * @param fs Filesystem path to content mappings
 * @param os Operating system info to emulate
 */
public class TestPlatformProvider(
    env: Map<String, String> = emptyMap(),
    private val props: Map<String, String> = emptyMap(),
    private val fs: Map<String, String> = emptyMap(),
    private val os: OperatingSystem = OperatingSystem(OsFamily.Linux, "test")
) : PlatformProvider, Filesystem by Filesystem.fromMap(fs.mapValues { it.value.encodeToByteArray() }) {

    public companion object;

    // ensure HOME directory is set for path normalization. this is mostly for AWS config loader behavior
    private val env = if (env.containsKey("HOME")) env else env.toMutableMap().apply { put("HOME", "/users/test") }
    override val filePathSeparator: String
        get() = when (os.family) {
            OsFamily.Windows -> "\\"
            else -> "/"
        }

    override fun osInfo(): OperatingSystem = os
    override fun getProperty(key: String): String? = props[key]
    override fun getenv(key: String): String? = env[key]
}
