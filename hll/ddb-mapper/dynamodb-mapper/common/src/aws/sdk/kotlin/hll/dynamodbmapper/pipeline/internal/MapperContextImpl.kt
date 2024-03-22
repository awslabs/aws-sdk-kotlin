/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal

import aws.sdk.kotlin.hll.dynamodbmapper.TableSpec
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.*

internal data class MapperContextImpl<T>(
    override val tableSpec: TableSpec<T>,
    override val operation: String,
) : MapperContext<T>
