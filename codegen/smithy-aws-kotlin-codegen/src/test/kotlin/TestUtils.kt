package aws.sdk.kotlin.codegen.awsjson

import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.build.MockManifest
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.kotlin.codegen.*
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.integration.HttpTraitResolver
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.TimestampFormatTrait

/**
 * Load and initialize a model from a String (from smithy-rs)
 */
fun String.asSmithyModel(sourceLocation: String? = null): Model {
    val processed = if (this.startsWith("\$version")) this else "\$version: \"1.0\"\n$this"
    return Model.assembler().discoverModels().addUnparsedModel(sourceLocation ?: "test.smithy", processed).assemble().unwrap()
}

// A ProtocolGenerator for tests.
class MockHttpProtocolGenerator : HttpBindingProtocolGenerator() {
    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS
    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        HttpTraitResolver(ctx, "application/json")

    override val protocol: ShapeId = RestJson1Trait.ID

    override fun generateProtocolUnitTests(ctx: ProtocolGenerator.GenerationContext) {}
}

// Produce a GenerationContext given a model, it's expected namespace and service name.
fun Model.generateTestContext(namespace: String, serviceName: String): ProtocolGenerator.GenerationContext {
    val packageNode = Node.objectNode().withMember("name", Node.from("test"))
        .withMember("version", Node.from("1.0.0"))

    val settings = KotlinSettings.from(
        this,
        Node.objectNodeBuilder()
            .withMember("package", packageNode)
            .build()
    )
    val provider: SymbolProvider = KotlinCodegenPlugin.createSymbolProvider(this, namespace, serviceName)
    val service = this.expectShape<ServiceShape>("$namespace#$serviceName")
    val generator: ProtocolGenerator = MockHttpProtocolGenerator()
    val manifest = MockManifest()
    val delegator = KotlinDelegator(settings, this, manifest, provider)

    return ProtocolGenerator.GenerationContext(
        settings,
        this,
        service,
        provider,
        listOf(),
        generator.protocol,
        delegator
    )
}

// Create and use a writer to drive codegen from a function taking a writer.
// Strip off comment and package preamble.
fun generateCode(generator: (KotlinWriter) -> Unit): String {
    val packageDeclaration = "some-unique-thing-that-will-never-be-codegened"
    val writer = KotlinWriter(packageDeclaration)
    generator.invoke(writer)
    val rawCodegen = writer.toString()
    return rawCodegen.substring(rawCodegen.indexOf(packageDeclaration) + packageDeclaration.length).trim()
}
