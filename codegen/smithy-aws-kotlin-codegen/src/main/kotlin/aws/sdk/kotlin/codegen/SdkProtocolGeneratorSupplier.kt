/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.customization.s3.isS3
import aws.sdk.kotlin.codegen.protocols.*
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Integration that registers protocol generators this package provides
 */
class SdkProtocolGeneratorSupplier : KotlinIntegration {
    /**
     * Gets the sort order of the customization from -128 to 127, with lowest
     * executed first.
     *
     * @return Returns the sort order, defaults to -10.
     */
    override val order: Byte = -10

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        // S3 gets it's own customized protocol generator
        val service = model.expectShape<ServiceShape>(settings.service)
        return !service.isS3
    }

    override val protocolGenerators: List<ProtocolGenerator> =
        listOf(
            AwsJson1_0(),
            AwsJson1_1(),
            RestJson1(),
            RestXml(),
            AwsQuery(),
            Ec2Query(),
        )
}
