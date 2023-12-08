/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.integration.AppendingSectionWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.rendering.ServiceClientGenerator

// FIXME - should this be pulled in by all AWS protocols and not just the AWS SDK
/**
 * Adds a section writer which applies an interceptor that detects and corrects clock skew
 */
class ClockSkew : KotlinIntegration {
    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(SectionWriterBinding(ServiceClientGenerator.Sections.FinalizeConfig, clockSkewSectionWriter))

    private val clockSkewSectionWriter = AppendingSectionWriter { writer ->
        val interceptorSymbol = RuntimeTypes.AwsProtocolCore.ClockSkewInterceptor
        writer.write("builder.config.interceptors.add(0, #T())", interceptorSymbol)
    }
}
