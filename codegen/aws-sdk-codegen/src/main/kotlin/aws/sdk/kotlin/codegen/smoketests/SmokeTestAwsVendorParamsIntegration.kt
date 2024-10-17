package aws.sdk.kotlin.codegen.smoketests

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.getContextValue
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.*
import software.amazon.smithy.kotlin.codegen.utils.topDownOperations
import software.amazon.smithy.model.Model
import software.amazon.smithy.smoketests.traits.SmokeTestsTrait

/**
 * Adds support for AWS specific client config during smoke tests code generation.
 */
class SmokeTestAwsVendorParamsIntegration : KotlinIntegration {
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
                useGlobalEndpointSectionWriters
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
