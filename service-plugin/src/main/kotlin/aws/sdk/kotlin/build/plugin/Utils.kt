/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.build.plugin

import org.gradle.api.Project
import org.gradle.api.logging.Logger

fun <T> Project.tryGetClass(className: String): Class<T>? {
    val classLoader = buildscript.classLoader
    return try {
        Class.forName(className, false, classLoader) as Class<T>
    } catch (e: ClassNotFoundException) {
        null
    }
}

fun <T> java.util.Optional<T>.orNull(): T? = this.orElse(null)

fun Logger.aws(message: String) {
//    info("aws: $message")
    warn("aws: $message")
}

