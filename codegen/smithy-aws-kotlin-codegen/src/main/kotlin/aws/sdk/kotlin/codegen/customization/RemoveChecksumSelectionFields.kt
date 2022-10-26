/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.aws.traits.HttpChecksumTrait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectTrait
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.model.shapes
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.transform.ModelTransformer
import java.util.logging.Logger

/**
 * Temporary integration to remove flexible checksum fields from models.
 * TODO https://github.com/awslabs/aws-sdk-kotlin/issues/557
 */
class RemoveChecksumSelectionFields : KotlinIntegration {
    private val logger = Logger.getLogger(javaClass.name)

    override val order: Byte = -127

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = model
        .shapes<OperationShape>()
        .any { it.hasTrait<HttpChecksumTrait>() }

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val dropMembers = model
            .shapes<OperationShape>()
            .filter { it.hasTrait<HttpChecksumTrait>() }
            .flatMap { op ->
                val trait = op.expectTrait<HttpChecksumTrait>()

                val requestAlgorithmMember = trait.requestAlgorithmMember.getOrNull()
                val requestValidationModeMember = trait.requestValidationModeMember.getOrNull()

                listOfNotNull(requestAlgorithmMember, requestValidationModeMember)
                    .map { findInputMember(model, op, it) }
            }
            .toSet()

        return ModelTransformer.create().filterShapes(model) { shape ->
            when (shape) {
                is MemberShape -> (shape !in dropMembers).also {
                    if (!it) {
                        logger.warning("Removed $shape from model because it is a flexible checksum member")
                    }
                }
                else -> true
            }
        }
    }
}

private fun findInputMember(model: Model, op: OperationShape, name: String): MemberShape =
    model.expectShape(op.inputShape).members().first { it.memberName == name }
