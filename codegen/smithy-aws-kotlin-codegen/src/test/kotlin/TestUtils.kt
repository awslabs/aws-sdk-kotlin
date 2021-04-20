package aws.sdk.kotlin.codegen.awsjson

import io.kotest.matchers.string.shouldContainOnlyOnce
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.build.MockManifest
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.kotlin.codegen.*
import software.amazon.smithy.kotlin.codegen.integration.*
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.TimestampFormatTrait

/**
 * Load and initialize a model from a String (from smithy-rs)
 */
fun String.asSmithyModel(sourceLocation: String? = null): Model {
    val processed = if (this.startsWith("\$version")) this else "\$version: \"1.0\"\n$this"
    return Model.assembler().discoverModels().addUnparsedModel(sourceLocation ?: "test.smithy", processed).assemble().unwrap()
}

class TestProtocolClientGenerator(
    ctx: ProtocolGenerator.GenerationContext,
    features: List<HttpFeature>,
    httpBindingResolver: HttpBindingResolver
) : HttpProtocolClientGenerator(ctx, features, httpBindingResolver) {
    override val serdeProviderSymbol: Symbol = buildSymbol {
        name = "JsonSerdeProvider"
        namespace(KotlinDependency.CLIENT_RT_SERDE_JSON)
    }
}

// A ProtocolGenerator for tests.
class MockHttpProtocolGenerator : HttpBindingProtocolGenerator() {
    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS
    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        HttpTraitResolver(ctx, "application/json")

    override val protocol: ShapeId = RestJson1Trait.ID

    override fun generateProtocolUnitTests(ctx: ProtocolGenerator.GenerationContext) {}
    override fun getHttpProtocolClientGenerator(ctx: ProtocolGenerator.GenerationContext): HttpProtocolClientGenerator =
        TestProtocolClientGenerator(ctx, getHttpFeatures(ctx), getProtocolHttpBindingResolver(ctx))

    override fun generateSdkFieldDescriptor(
        ctx: ProtocolGenerator.GenerationContext,
        memberShape: MemberShape,
        writer: KotlinWriter,
        memberTargetShape: Shape?,
        namePostfix: String
    ) {
    }

    override fun generateSdkObjectDescriptorTraits(
        ctx: ProtocolGenerator.GenerationContext,
        objectShape: Shape,
        writer: KotlinWriter
    ) {
    }
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

fun String.generateTestModel(
    protocol: String,
    namespace: String = "com.test",
    serviceName: String = "Example",
    operations: List<String> = listOf("Foo")
): Model {
    val completeModel = """
        namespace $namespace

        use aws.protocols#$protocol

        @$protocol
        service $serviceName {
            version: "1.0.0",
            operations: [
                ${operations.joinToString(separator = ", ")}
            ]
        }
        
        $this
    """.trimIndent()

    return completeModel.asSmithyModel()
}

fun codegenTestHarnessForModelSnippet(
    generator: ProtocolGenerator,
    namespace: String = "com.test",
    serviceName: String = "Example",
    operations: List<String> = listOf("Foo"),
    snippet: () -> String
): CodegenTestHarness {
    val protocol = generator.protocol.name
    val model = snippet().generateTestModel(protocol, namespace, serviceName, operations)
    val ctx = model.generateTestContext(namespace, serviceName)
    val manifest = ctx.delegator.fileManifest as MockManifest

    return CodegenTestHarness(ctx, manifest, generator, namespace, serviceName, protocol)
}

fun String.formatForTest(indent: String = "    ") =
    trimIndent()
        .prependIndent(indent)
        .split('\n')
        .map { if (it.isBlank()) "" else it }
        .joinToString(separator = "\n") { it }

// Will generate an IDE diff in the case of a test assertion failure.
fun String?.shouldContainOnlyOnceWithDiff(expected: String) {
    try {
        this.shouldContainOnlyOnce(expected)
    } catch (originalException: AssertionError) {
        kotlin.test.assertEquals(expected, this) // no need to rethrow as this will throw
    }
}

/**
 * Contains references to all types necessary to drive and validate codegen.
 */
data class CodegenTestHarness(
    val generationCtx: ProtocolGenerator.GenerationContext,
    val manifest: MockManifest,
    val generator: ProtocolGenerator,
    val namespace: String,
    val serviceName: String,
    val protocol: String
)

// Drive de/serializer codegen and return results in map indexed by filename.
internal fun CodegenTestHarness.generateDeSerializers(): Map<String, String> {
    generator.generateSerializers(generationCtx)
    generator.generateDeserializers(generationCtx)
    generationCtx.delegator.flushWriters()
    return manifest.files.map { path -> path.fileName.toString() to manifest.expectFileString(path) }.toMap()
}
