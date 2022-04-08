package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.protocols.middleware.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigPropertyType
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model

class AwsSignerIntegration : KotlinIntegration {
    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ClientConfigProperty> = listOf(
        ClientConfigProperty {
            symbol = RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigner
            name = "signer"
            documentation = "The implementation of AWS signer to use for signing requests"
            propertyType = ClientConfigPropertyType.RequiredWithDefault("CrtAwsSigner")
            additionalImports = listOf(
                RuntimeTypes.Auth.Signing.AwsSigningCrt.CrtAwsSigner,
            )
        },
    )

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>
    ): List<ProtocolMiddleware> {
        val serviceName = AwsSignatureVersion4.signingServiceName(ctx.service)
        return resolved + AwsSignatureVersion4(serviceName)
    }

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        AwsSignatureVersion4.isSupportedAuthentication(model, settings.getService(model))
}
