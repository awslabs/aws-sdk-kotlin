/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.getContextValue
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.ExceptionBaseClassGenerator
import software.amazon.smithy.kotlin.codegen.rendering.ServiceExceptionBaseClassGenerator

/**
 * Integration that updates the generated service exception base class (e.g. `S3Exception`, `DynamoDbException`, etc)
 * to inherit from `AwsServiceException` instead of just `ServiceException`.
 */
class ReplaceServiceExceptionBase : KotlinIntegration {
    // S3 further customizes this by overriding the error metadata. See [aws.sdk.kotlin.codegen.customization.s3.S3ErrorMetadataIntegration]
    override val order: Byte = -10
    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(SectionWriterBinding(ExceptionBaseClassGenerator.ExceptionBaseClassSection, exceptionSectionWriter))

    private val exceptionSectionWriter = SectionWriter { writer, _ ->
        val ctx = writer.getContextValue(ExceptionBaseClassGenerator.ExceptionBaseClassSection.CodegenContext)
        ServiceExceptionBaseClassGenerator(exceptionBaseClassSymbol).render(ctx, writer)
    }

    private val exceptionBaseClassSymbol: Symbol = buildSymbol {
        name = "AwsServiceException"
        namespace(AwsKotlinDependency.AWS_CORE)
    }
}
