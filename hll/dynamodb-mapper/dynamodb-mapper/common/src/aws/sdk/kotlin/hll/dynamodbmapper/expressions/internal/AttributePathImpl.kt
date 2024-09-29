/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AttrPathElement
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AttributePath

internal data class AttrPathNameImpl(override val name: String) : AttrPathElement.Name

internal data class AttrPathIndexImpl(override val index: Int) : AttrPathElement.Index

internal data class AttributePathImpl(
    override val element: AttrPathElement,
    override val parent: AttributePath? = null,
) : AttributePath {
    init {
        require(element is AttrPathElement.Name || parent != null) {
            "Top-level attribute paths must be names (not indices)"
        }
    }
}

/**
 * An abstract attribute path that represents the sort key in a given schema. This isn't a concrete path and will be
 * replaced by the schema's _actual_ sort key in [KeyFilterImpl.toExpression].
 */
internal data object SkAttrPathImpl : AttributePath {
    override val element get() = error("Unsupported")
    override val parent get() = error("Unsupported")
}
