/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.customization.glacier

import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.defaultValue
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.model.traits.OperationInput
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.Shape

class GlacierAccountIdDefault : KotlinIntegration {
    override val order: Byte = -127
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).sdkId.equals("Glacier", ignoreCase = true)

    override fun decorateSymbolProvider(
        settings: KotlinSettings,
        model: Model,
        symbolProvider: SymbolProvider
    ): SymbolProvider = GlacierSymbolProvider(model, symbolProvider)
}

private class GlacierSymbolProvider(
    private val model: Model,
    private val symbolProvider: SymbolProvider
) : SymbolProvider by symbolProvider {

    override fun toSymbol(shape: Shape?): Symbol {
        val symbol = symbolProvider.toSymbol(shape)

        if (shape is MemberShape) {
            val container = model.expectShape(shape.container)
            if (container.hasTrait<OperationInput>() && shape.memberName.lowercase() == "accountid") {
                return symbol.toBuilder().defaultValue("\"-\"").build()
            }
        }

        return symbol
    }
}
