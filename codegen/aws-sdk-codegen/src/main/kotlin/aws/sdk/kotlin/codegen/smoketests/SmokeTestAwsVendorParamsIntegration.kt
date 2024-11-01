package aws.sdk.kotlin.codegen.smoketests

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.*
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.Param.ParamName
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.Param.ParamShape
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.Param.Parameter
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.Param.SymbolProvider
import software.amazon.smithy.kotlin.codegen.rendering.util.format
import software.amazon.smithy.kotlin.codegen.utils.dq
import software.amazon.smithy.kotlin.codegen.utils.topDownOperations
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.*
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.smoketests.traits.SmokeTestsTrait

/**
 * Code generates AWS specific code for smoke test runners
 */
class AwsSmokeTestsRunnerGeneratorIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.topDownOperations(settings.service).any { it.hasTrait<SmokeTestsTrait>() }

    override val sectionWriters: List<SectionWriterBinding>
        get() =
            dualStackSectionWriters +
                sigv4aRegionSetSectionWriters +
                uriSectionWriters +
                accountIdEndpointSectionWriters +
                regionSectionWriters +
                useAccelerateSectionWriters +
                useMultiRegionAccessPointsSectionWriters +
                useGlobalEndpointSectionWriters +
                listOf(parameterGenerator) // TODO: Convert this so that all can be placed here in this list
}

/**
 * TODO: Write
 */
private val parameterGenerator =
    SectionWriterBinding(Param) { writer, _ ->
        val paramName = writer.getContextValue(ParamName)
        val parameter = writer.getContextValue(Parameter)
        val shape = writer.getContextValue(ParamShape)

        val symbolProvider = writer.getContextValue(SymbolProvider)

        coerceParameterToModeledShape(
            parameter,
            shape,
            paramName,
            writer,
            symbolProvider,
        )
    }

fun coerceParameterToModeledShape(
    param: Node,
    customShape: Shape?,
    name: String,
    writer: KotlinWriter,
    symbolProvider: SymbolProvider
) {
    if (customShape == null || customShape.isCompatibleWithNodeTypes) {
        writer.writeInline("#L", param.format())
    }

    if (customShape != null) writer.write("// ${customShape::class.simpleName}")

    when (customShape) {
        is DocumentShape -> {} // TODO: Unknown !
        is BlobShape -> {} // TODO: Unknown !
        is EnumShape -> {
            val enumSymbol = symbolProvider.toSymbol(customShape)
            val enumValue = param.asStringNode().get().value

            writer.write("#T.#L", enumSymbol, enumValue)
        }
        is MemberShape -> {} // TODO: Important !
        is TimestampShape -> {} // TODO: Unknown !
        is UnionShape -> {} // TODO: Important !
//        else -> throw Exception("Code generation unsupported for smoke test operation parameter '$name' of type '$customShape'.")
    }
}

private val Shape.isCompatibleWithNodeTypes: Boolean
    get() = when (this) {
        is NumberShape, // TODO: Maybe not all numbers are okay?
        is MapShape,
        is StringShape,
        is BooleanShape, // TODO: One of these can actually be an enum.....
        is CollectionShape -> true

        else -> false
    }














/**
 * Uses the AWS Kotlin SDK specific name for the dual stack config option i.e. `useDualstack` -> `useDualStack`
 */
private val dualStackSectionWriters = listOf(
    SectionWriterBinding(SmokeTestUseDualStackKey) { writer, _ -> writer.writeInline("useDualStack") },
)

/**
 * Uses the AWS Kotlin SDK specific name for the sigV4a signing region set option i.e.
 * `sigv4aRegionSet` -> `sigV4aSigningRegionSet`.
 *
 * Converts the modeled sigV4a signing region set from a list to a set.
 *
 * Adds additional config needed for the SDK to make sigV4a calls
 */
private val sigv4aRegionSetSectionWriters = listOf(
    SectionWriterBinding(SmokeTestSigv4aRegionSetKey) { writer, _ -> writer.writeInline("sigV4aSigningRegionSet") },
    SectionWriterBinding(SmokeTestSigv4aRegionSetValue) { writer, value ->
        writer.write("#L.toSet()", value)
        // TODO: Remove once sigV4a is supported for default signer.
        writer.write(
            "authSchemes = listOf(#T(#T))",
            RuntimeTypes.Auth.HttpAuthAws.SigV4AsymmetricAuthScheme,
            RuntimeTypes.Auth.AwsSigningCrt.CrtAwsSigner,
        )
    },
)

/**
 * Ensures we use the provided URI
 */
private val uriSectionWriters = listOf(
    SectionWriterBinding(SmokeTestUriKey) { writer, _ -> writer.writeInline("endpointProvider") },
    SectionWriterBinding(SmokeTestUriValue) { writer, value ->
        val endpointProvider = writer.getContextValue(SmokeTestUriValue.EndpointProvider)
        val endpointParameters = writer.getContextValue(SmokeTestUriValue.EndpointParameters)
        writer.withBlock("object : #T {", "}", endpointProvider) {
            write(
                "override suspend fun resolveEndpoint(params: #T): #T = #T(#L)",
                endpointParameters,
                RuntimeTypes.SmithyClient.Endpoints.Endpoint,
                RuntimeTypes.SmithyClient.Endpoints.Endpoint,
                value,
            )
        }
    },
)

/**
 * Uses the AWS Kotlin SDK specific way of configuring `accountIdEndpointMode`
 */
private val accountIdEndpointSectionWriters = listOf(
    SectionWriterBinding(SmokeTestAccountIdBasedRoutingKey) { writer, _ -> writer.writeInline("accountIdEndpointMode") },
    SectionWriterBinding(SmokeTestAccountIdBasedRoutingValue) { writer, value ->
        when (value) {
            "true" -> writer.write("#T.REQUIRED", AwsRuntimeTypes.Config.Endpoints.AccountIdEndpointMode)
            "false" -> writer.write("#T.DISABLED", AwsRuntimeTypes.Config.Endpoints.AccountIdEndpointMode)
        }
    },
)

/**
 * Gets region override environment variable.
 *
 * Sets region override as default.
 *
 * Sets region override as default client config if no other client config is modelled.
 */
private val regionSectionWriters = listOf(
    SectionWriterBinding(SmokeTestAdditionalEnvVars) { writer, _ ->
        writer.write(
            "private val regionOverride = #T.System.getenv(#S)",
            RuntimeTypes.Core.Utils.PlatformProvider,
            "AWS_SMOKE_TEST_REGION",
        )
    },
    SectionWriterBinding(SmokeTestRegionDefault) { writer, _ ->
        writer.writeInline("regionOverride ?: ")
    },
    SectionWriterBinding(SmokeTestDefaultConfig) { writer, _ -> writer.write("region = regionOverride") },
)

/**
 * Uses the AWS Kotlin SDK specific name for the S3 accelerate config option i.e. `useAccelerate` -> `enableAccelerate`
 */
private val useAccelerateSectionWriters = listOf(
    SectionWriterBinding(SmokeTestUseAccelerateKey) { writer, _ -> writer.writeInline("enableAccelerate") },
)

/**
 * Uses the AWS Kotlin SDK specific name for the S3 multi region access points (MRAP) config option i.e.
 * `useMultiRegionAccessPoints` -> `disableMrap`.
 *
 * Our config option is opt out while the modeled config is opt in so we invert the boolean values.
 */
private val useMultiRegionAccessPointsSectionWriters = listOf(
    SectionWriterBinding(SmokeTestUseMultiRegionAccessPointsKey) { writer, _ -> writer.writeInline("disableMrap") },
    SectionWriterBinding(SmokeTestUseMultiRegionAccessPointsValue) { writer, value ->
        when (value) {
            "true" -> writer.write("false")
            "false" -> writer.write("true")
        }
    },
)

/**
 * Leaves a comment in the client config whenever the use of the `useGlobalEndpoint` S3 config option is modeled.
 *
 * The SDK does not support this config option.
 * See `BindAwsEndpointBuiltins` & `S3_USE_GLOBAL_ENDPOINT`
 */
private val useGlobalEndpointSectionWriters = listOf(
    SectionWriterBinding(SmokeTestUseGlobalEndpoint) { writer, _ ->
        writer.write("// Smoke tests modeled the use of `useGlobalEndpoint` config, but it's not supported by the SDK")
    },
)
