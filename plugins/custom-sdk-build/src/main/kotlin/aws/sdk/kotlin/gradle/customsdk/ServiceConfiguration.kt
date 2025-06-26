/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

/**
 * Base class for service configurations.
 * Contains the selected operations for a specific AWS service.
 */
abstract class ServiceConfiguration {
    /**
     * The operations selected for this service.
     */
    internal val selectedOperations = mutableListOf<OperationConstant>()
    
    /**
     * Get the selected operations as a list of shape IDs.
     */
    fun getSelectedOperations(): List<String> {
        return selectedOperations.map { it.shapeId }
    }
}
