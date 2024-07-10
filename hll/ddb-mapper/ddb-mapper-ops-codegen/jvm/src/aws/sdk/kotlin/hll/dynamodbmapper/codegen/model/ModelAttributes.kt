/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.smithy.kotlin.runtime.collections.AttributeKey

internal object ModelAttributes {
    val LowLevelOperation: AttributeKey<Operation> = AttributeKey("aws.sdk.kotlin.ddbmapper#LowLevelOperation")
    val LowLevelStructure: AttributeKey<Structure> = AttributeKey("aws.sdk.kotlin.ddbmapper#LowLevelStructure")
}
