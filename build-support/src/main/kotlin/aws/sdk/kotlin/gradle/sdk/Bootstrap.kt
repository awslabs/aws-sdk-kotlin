/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk

import org.gradle.api.Plugin
import org.gradle.api.Project

// Dummy plugin, we use a plugin because it's easiest with an included build to apply to a buildscript and get
// the buildscript classpath correct.
class Bootstrap : Plugin<Project> {
    override fun apply(project: Project) {}
}
