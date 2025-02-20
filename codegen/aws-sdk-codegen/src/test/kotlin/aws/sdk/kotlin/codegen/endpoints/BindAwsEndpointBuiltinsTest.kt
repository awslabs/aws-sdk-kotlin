package aws.sdk.kotlin.codegen.endpoints

import software.amazon.smithy.build.MockManifest
import software.amazon.smithy.kotlin.codegen.KotlinCodegenPlugin
import software.amazon.smithy.kotlin.codegen.core.GenerationContext
import software.amazon.smithy.kotlin.codegen.core.KotlinDelegator
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.test.*
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType
import kotlin.test.Test
import kotlin.test.assertTrue

class BindAwsEndpointBuiltinsTest {
    @Test
    fun testRenderAccountIdEndpointModeBuiltin() {
        val model = "".prependNamespaceAndService().toSmithyModel()
        val serviceName = TestModelDefault.SERVICE_NAME
        val packageName = TestModelDefault.NAMESPACE
        val settings = model.defaultSettings(serviceName, packageName)
        val generator = MockHttpProtocolGenerator(model)
        val integrations = emptyList<KotlinIntegration>()
        val manifest = MockManifest()
        val provider = KotlinCodegenPlugin.createSymbolProvider(model, settings = settings)
        val service = model.getShape(ShapeId.from("$packageName#$serviceName")).get().asServiceShape().get()
        val delegator = KotlinDelegator(settings, model, manifest, provider, integrations)

        val ctx = ProtocolGenerator.GenerationContext(
            settings,
            model,
            service,
            provider,
            integrations,
            generator.protocol,
            delegator,
        )
        val writer = KotlinWriter(TestModelDefault.NAMESPACE)
        val parameters = listOf(
            Parameter
                .builder()
                .builtIn(AwsBuiltins.ACCOUNT_ID_ENDPOINT_MODE)
                .type(ParameterType.STRING)
                .name("accountIdEndpointMode")
                .build(),
        )

        renderBindAwsBuiltins(
            ctx,
            writer,
            parameters,
        )

        assertTrue(
            writer
                .rawString()
                .contains("accountIdEndpointMode = config.accountIdEndpointMode.toString().lowercase()"),
        )
    }
}
