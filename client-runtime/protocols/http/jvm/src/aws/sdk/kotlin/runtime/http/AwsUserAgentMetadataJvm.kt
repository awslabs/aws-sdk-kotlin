/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http

import aws.smithy.kotlin.runtime.util.Platform

internal actual fun platformLanguageMetadata(): LanguageMetadata {
    val jvmMetadata = mutableMapOf(
        "javaVersion" to getSystemProperty("java.version"),
        "jvmName" to getSystemProperty("java.vm.name"),
        "jvmVersion" to getSystemProperty("java.vm.version")
    )

    if (Platform.isAndroid) {
        // https://developer.android.com/reference/android/os/Build.VERSION
        val buildVersionCls = Class.forName("android.os.Build\$VERSION")
        val sdkIntField = buildVersionCls.getDeclaredField("SDK_INT")
        val sdkReleaseField = buildVersionCls.getDeclaredField("RELEASE")

        jvmMetadata["androidApiVersion"] = sdkIntField.getInt(null).toString()
        jvmMetadata["androidRelease"] = sdkReleaseField.get(null) as String
    }

    return LanguageMetadata(extras = jvmMetadata)
}

private fun getSystemProperty(name: String, defaultValue: String = "unknown"): String =
    runCatching { System.getProperty(name) }.getOrDefault(defaultValue)
