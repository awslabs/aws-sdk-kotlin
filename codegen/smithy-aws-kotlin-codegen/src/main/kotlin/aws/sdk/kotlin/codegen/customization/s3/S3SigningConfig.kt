/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.protocols.core.AwsHttpProtocolClientGenerator
import aws.sdk.kotlin.codegen.protocols.core.putIfAbsent
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Set default signing attributes in the execution context (which ultimately becomes the signing context) for S3.
 */
class S3SigningConfig : KotlinIntegration {
    // auth schemes are de-duped by taking the last one registered
    override val order: Byte
        get() = 127

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).isS3

    override val sectionWriters: List<SectionWriterBinding> = listOf(
        SectionWriterBinding(AwsHttpProtocolClientGenerator.MergeServiceDefaults, ::renderDefaultSigningContext),
    )
    private fun renderDefaultSigningContext(writer: KotlinWriter, previousValue: String?) {
        val signingAttrs = RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningAttributes
        // https://github.com/awslabs/aws-sdk-kotlin/issues/200
        writer.putIfAbsent(signingAttrs, "NormalizeUriPath", "false")
        writer.putIfAbsent(signingAttrs, "UseDoubleUriEncode", "false")
        writer.putIfAbsent(signingAttrs, "SignedBodyHeader", writer.format("#T.X_AMZ_CONTENT_SHA256", RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSignedBodyHeader))
    }
}
