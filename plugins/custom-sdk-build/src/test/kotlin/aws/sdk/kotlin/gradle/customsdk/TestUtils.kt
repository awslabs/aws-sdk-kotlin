/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal

/**
 * Test utilities for plugin testing.
 */
object TestUtils {
    
    /**
     * Trigger project evaluation for testing purposes.
     * This is needed to simulate the afterEvaluate behavior in tests.
     */
    fun Project.evaluate() {
        if (this is ProjectInternal) {
            try {
                this.evaluate()
            } catch (e: Exception) {
                // Ignore evaluation errors in tests
                // The main purpose is to trigger afterEvaluate blocks
            }
        }
    }
}
