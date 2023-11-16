/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.AbstractShapeBuilder
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.DefaultTrait
import software.amazon.smithy.model.transform.ModelTransformer
import software.amazon.smithy.utils.ToSmithyBuilder

/**
 * Removes the default value of certain shapes, and any member that target
 * those shapes,for certain services. These default values may cause
 * serialization, validation, or other unexpected issues.
 */
class RemoveDefaults : KotlinIntegration {
    // Service shape id -> Shape id of each root shape to remove default values from.
    private val removeDefaultsFrom = mapOf(
        ShapeId.from("com.amazonaws.amplifyuibuilder#AmplifyUIBuilder") to setOf(
            ShapeId.from("com.amazonaws.amplifyuibuilder#ListComponentsLimit"),
            ShapeId.from("com.amazonaws.amplifyuibuilder#ListFormsLimit"),
            ShapeId.from("com.amazonaws.amplifyuibuilder#ListThemesLimit"),
        ),
        ShapeId.from("com.amazonaws.drs#ElasticDisasterRecoveryService") to setOf(
            ShapeId.from("com.amazonaws.drs#Validity"),
            ShapeId.from("com.amazonaws.drs#CostOptimizationConfiguration\$burstBalanceThreshold"),
            ShapeId.from("com.amazonaws.drs#CostOptimizationConfiguration\$burstBalanceDeltaThreshold"),
            ShapeId.from("com.amazonaws.drs#ListStagingAccountsRequest\$maxResults"),
            ShapeId.from("com.amazonaws.drs#StrictlyPositiveInteger"),
            ShapeId.from("com.amazonaws.drs#MaxResultsType"),
            ShapeId.from("com.amazonaws.drs#MaxResultsReplicatingSourceServers"),
            ShapeId.from("com.amazonaws.drs#LaunchActionOrder"),
        ),
        ShapeId.from("com.amazonaws.evidently#Evidently") to setOf(
            ShapeId.from("com.amazonaws.evidently#ResultsPeriod"),
        ),
        ShapeId.from("com.amazonaws.location#LocationService") to setOf(
            ShapeId.from("com.amazonaws.location#ListPlaceIndexesRequest\$MaxResults"),
            ShapeId.from("com.amazonaws.location#SearchPlaceIndexForSuggestionsRequest\$MaxResults"),
            ShapeId.from("com.amazonaws.location#PlaceIndexSearchResultLimit"),
        ),
        ShapeId.from("com.amazonaws.paymentcryptographydata#PaymentCryptographyDataPlane") to setOf(
            ShapeId.from("com.amazonaws.paymentcryptographydata#IntegerRangeBetween4And12"),
        ),
        ShapeId.from("com.amazonaws.emrserverless#AwsToledoWebService") to setOf(
            ShapeId.from("com.amazonaws.emrserverless#WorkerCounts"),
        ),
    )

    override val order: Byte = 0

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        settings.service in removeDefaultsFrom

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val serviceId = settings.service
        val removeDefaultsFromShapes = removeDefaultsFrom[serviceId]
            ?: throw CodegenException("expected $serviceId in removed defaults map")
        return removeDefaults(model, removeDefaultsFromShapes)
    }

    fun removeDefaults(model: Model, fromShapes: Set<ShapeId>): Model {
        val removedRootDefaults: MutableSet<ShapeId> = HashSet()
        val removedRootDefaultsModel = ModelTransformer.create().mapShapes(model) {
            if (shouldRemoveRootDefault(it, fromShapes)) {
                removedRootDefaults.add(it.id)
                removeDefault(it)
            } else {
                it
            }
        }

        return ModelTransformer.create().mapShapes(removedRootDefaultsModel) {
            if (shouldRemoveMemberDefault(it, removedRootDefaults, fromShapes)) {
                removeDefault(it)
            } else {
                it
            }
        }
    }

    private fun shouldRemoveRootDefault(shape: Shape, removeDefaultsFrom: Set<ShapeId>): Boolean =
        shape !is MemberShape && shape.id in removeDefaultsFrom && shape.hasTrait<DefaultTrait>()

    private fun shouldRemoveMemberDefault(
        shape: Shape,
        removedRootDefaults: Set<ShapeId>,
        removeDefaultsFrom: Set<ShapeId>,
    ): Boolean = shape is MemberShape &&
        // Check the original set of shapes to remove for this shape id, to remove members that were in that set.
        (shape.target in removedRootDefaults || shape.id in removeDefaultsFrom) &&
        shape.hasTrait<DefaultTrait>()

    private fun removeDefault(shape: Shape): Shape =
        ((shape as ToSmithyBuilder<*>).toBuilder() as AbstractShapeBuilder<*, *>)
            .removeTrait(DefaultTrait.ID)
            .build()
}
