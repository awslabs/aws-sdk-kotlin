/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.hll.codegen.model.Type
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Describes a member (i.e., component field, attribute, property, etc.) of a [Structure]
 * @param name The name of the member inside its parent [Structure]
 * @param type The [Type] of the member
 */
data class Member(val name: String, val type: Type) {
    companion object {
        /**
         * Derive a [Member] from a [KSPropertyDeclaration]
         */
        fun from(prop: KSPropertyDeclaration) = Member(
            name = prop.simpleName.getShortName(),
            type = Type.from(prop.type),
        )
    }
}
