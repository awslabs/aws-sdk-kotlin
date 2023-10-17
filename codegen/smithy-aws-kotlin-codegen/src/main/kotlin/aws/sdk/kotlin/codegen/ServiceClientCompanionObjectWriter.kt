/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.rendering.ServiceClientGenerator
import software.amazon.smithy.kotlin.codegen.utils.toPascalCase

/**
 * Overrides the service client companion object for how a client is constructed.
 *
 * Includes the ability to extend the config finalizer, which by default handles resolution of endpoint url config.
 */
class ServiceClientCompanionObjectWriter(
    private val extendFinalizeConfig: (KotlinWriter.() -> Unit)? = null,
) : SectionWriter {
    override fun write(writer: KotlinWriter, previousValue: String?) {
        val serviceSymbol = writer.getContextValue(ServiceClientGenerator.Sections.CompanionObject.ServiceSymbol)

        writer.withBlock(
            "public companion object : #T<Config, Config.Builder, #T, Builder>() {",
            "}",
            AwsRuntimeTypes.Config.AbstractAwsSdkClientFactory,
            serviceSymbol,
        ) {
            writeBuilder()
            write("")

            writeFinalizeConfig()
        }
    }

    private fun KotlinWriter.writeBuilder() {
        write("@#T", KotlinTypes.Jvm.JvmStatic)
        write("override fun builder(): Builder = Builder()")
    }

    private fun KotlinWriter.writeFinalizeConfig() {
        withBlock(
            "override suspend fun finalizeConfig(builder: Builder, sharedConfig: #T<#T>) {",
            "}",
            RuntimeTypes.Core.Utils.LazyAsyncValue,
            AwsRuntimeTypes.Config.Profile.AwsSharedConfig,
        ) {
            declareSection(ServiceClientGenerator.Sections.FinalizeConfig)
            writeResolveEndpointUrl()
            extendFinalizeConfig?.let {
                write("")
                it()
            }
        }
    }

    private fun KotlinWriter.writeResolveEndpointUrl() {
        withBlock(
            "builder.config.endpointUrl = builder.config.endpointUrl ?: #T(",
            ")",
            AwsRuntimeTypes.Config.Endpoints.resolveEndpointUrl,
        ) {
            val sdkId = getContextValue(ServiceClientGenerator.Sections.CompanionObject.SdkId)
            val names = sdkId.toEndpointUrlConfigNames()

            write("sharedConfig,")
            write("#S,", names.sysPropSuffix)
            write("#S,", names.envSuffix)
            write("#S,", names.sharedConfigKey)
        }
    }
}

internal data class EndpointUrlConfigNames(
    val sysPropSuffix: String,
    val envSuffix: String,
    val sharedConfigKey: String,
)

internal fun String.toEndpointUrlConfigNames(): EndpointUrlConfigNames = EndpointUrlConfigNames(
    withTransform(JvmSystemPropertySuffix),
    withTransform(SdkIdTransform.UpperSnakeCase),
    withTransform(SdkIdTransform.LowerSnakeCase),
)

// JVM system property names follow the pattern "aws.endpointUrl${BaseClientName}"
// where BaseClientName is the PascalCased sdk ID with any forbidden suffixes dropped - this is the same as what we use
// for our client names
// e.g. sdkId "Elasticsearch Service" -> client name "ElasticsearchClient", prop "aws.endpointUrlElasticsearch"
private object JvmSystemPropertySuffix : SdkIdTransformer {
    override fun transform(id: String): String =
        id.toPascalCase().removeSuffix("Service")
}
