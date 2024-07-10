/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import com.google.devtools.ksp.symbol.KSPropertyDeclaration

internal data class Member(val name: String, val type: Type) {
    companion object {
        fun from(prop: KSPropertyDeclaration) = Member(
            name = prop.simpleName.getShortName(),
            type = Type.from(prop.type),
        )
    }
}
