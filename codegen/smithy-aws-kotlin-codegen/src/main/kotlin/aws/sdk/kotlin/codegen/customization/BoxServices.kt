/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.isNumberShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.neighbor.Walker
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.transform.ModelTransformer
import software.amazon.smithy.utils.ToSmithyBuilder

/**
 * Integration that pre-processes the model to box all unboxed primitives.
 *
 * See: https://github.com/awslabs/aws-sdk-kotlin/issues/261
 *
 * EC2 incorrectly models primitive shapes as unboxed when they actually
 * need to be boxed for the API to work properly (e.g. sending default values). The
 * rest of these services are at risk of similar behavior because they aren't true coral services
 */
class BoxServices : KotlinIntegration {
    override val order: Byte = -127

    private val serviceIds = listOf(
        "com.amazonaws.ec2#AmazonEC2",
        "com.amazonaws.nimble#nimble",
        "com.amazonaws.amplifybackend#AmplifyBackend",
        "com.amazonaws.apigatewaymanagementapi#ApiGatewayManagementApi",
        "com.amazonaws.apigatewayv2#ApiGatewayV2",
        "com.amazonaws.dataexchange#DataExchange",
        "com.amazonaws.greengrass#Greengrass",
        "com.amazonaws.iot1clickprojects#AWSIoT1ClickProjects",
        "com.amazonaws.kafka#Kafka",
        "com.amazonaws.macie2#Macie2",
        "com.amazonaws.mediaconnect#MediaConnect",
        "com.amazonaws.mediaconvert#MediaConvert",
        "com.amazonaws.medialive#MediaLive",
        "com.amazonaws.mediapackage#MediaPackage",
        "com.amazonaws.mediapackagevod#MediaPackageVod",
        "com.amazonaws.mediatailor#MediaTailor",
        "com.amazonaws.pinpoint#Pinpoint",
        "com.amazonaws.pinpointsmsvoice#PinpointSMSVoice",
        "com.amazonaws.serverlessapplicationrepository#ServerlessApplicationRepository",
        "com.amazonaws.mq#mq",
        "com.amazonaws.schemas#schemas",
    ).map(ShapeId::from)

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        serviceIds.any { it == settings.service }

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val serviceClosure = Walker(model).walkShapes(model.expectShape(settings.service))

        return ModelTransformer.create().mapShapes(model) {
            if (it in serviceClosure && !it.id.namespace.startsWith("smithy.api")) {
                boxPrimitives(model, it)
            } else {
                it
            }
        }
    }

    private fun boxPrimitives(model: Model, shape: Shape): Shape {
        val target = when (shape) {
            is MemberShape -> model.expectShape(shape.target)
            else -> shape
        }

        return when {
            shape is MemberShape && target.isPrimitiveShape -> box(shape)
            shape is NumberShape -> boxNumber(shape)
            shape is BooleanShape -> box(shape)
            else -> shape
        }
    }

    private val Shape.isPrimitiveShape: Boolean
        get() = isBooleanShape || isNumberShape

    private fun <T> box(shape: T): Shape where T : Shape, T : ToSmithyBuilder<T> =
        (shape.toBuilder() as AbstractShapeBuilder<*, T>)
            .addTrait(@Suppress("DEPRECATION") software.amazon.smithy.model.traits.BoxTrait())
            .build()

    private fun boxNumber(shape: NumberShape): Shape = when (shape) {
        is ByteShape -> box(shape)
        is IntegerShape -> box(shape)
        is LongShape -> box(shape)
        is ShortShape -> box(shape)
        is FloatShape -> box(shape)
        is DoubleShape -> box(shape)
        is BigDecimalShape -> box(shape)
        is BigIntegerShape -> box(shape)
        else -> throw CodegenException("unhandled numeric shape: $shape")
    }
}
