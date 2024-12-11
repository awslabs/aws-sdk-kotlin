package aws.sdk.kotlin.codegen.customization.flexiblechecksums.s3

import aws.sdk.kotlin.codegen.AwsRuntimeTypes.Http.Interceptors.S3CompositeChecksumsInterceptor
import aws.sdk.kotlin.codegen.customization.s3.isS3
import software.amazon.smithy.aws.traits.HttpChecksumTrait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Removes composite checksums returned by S3 so that flexible checksums won't validate them.
 * Composite checksums are used for multipart uploads and end with "-#" where "#" is a number.
 */
class S3CompositeChecksumsIntegration : KotlinIntegration {
    override val order: Byte
        get() = -128

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).isS3

    override fun customizeMiddleware(ctx: ProtocolGenerator.GenerationContext, resolved: List<ProtocolMiddleware>) =
        resolved + s3CompositeChecksumsMiddleware
}

/**
 * Adds [S3CompositeChecksumsInterceptor] to interceptors when operation has flexible checksums.
 */
private val s3CompositeChecksumsMiddleware = object : ProtocolMiddleware {
    override val name: String = "S3CompositeChecksumsMiddleware"

    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
        op.hasTrait<HttpChecksumTrait>()

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.write("op.interceptors.add(#T())", S3CompositeChecksumsInterceptor)
    }
}
