/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http

internal actual fun platformLanguageMetadata(): LanguageMetadata {
    val jvmMetadata = mutableMapOf<String, String>(
        "jvmName" to System.getProperty("java.vm.name"),
        "jvmVersion" to System.getProperty("java.vm.version")
    )

    return LanguageMetadata(extras = jvmMetadata)
}
