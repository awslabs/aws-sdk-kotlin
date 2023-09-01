/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.getContextValue
import software.amazon.smithy.kotlin.codegen.core.withBlock
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
    withTransform(JVMSystemPropertySuffix),
    withTransform(SdkIdTransform.UpperSnakeCase),
    withTransform(SdkIdTransform.LowerSnakeCase),
)

// JVM system property names follow those used by the Java SDK, which for service-specific endpoint URL config uses the
// pattern "aws.endpointUrl${BaseClientName}"
//
// this is an arbitrary sdkId transform to replicate the derivation of BaseClientName - see
// https://github.com/aws/aws-sdk-java-v2/blob/master/codegen/src/main/java/software/amazon/awssdk/codegen/naming/DefaultNamingStrategy.java#L116
private object JVMSystemPropertySuffix : SdkIdTransformer {
    override fun transform(id: String): String =
        id.toPascalCase().removePrefix("Amazon").removePrefix("Aws").removeSuffix("Service")
}
