package smithy.kotlin.codegen

import smithy.kotlin.codegen.generators.ServiceDefinitionGenerator
import smithy.kotlin.codegen.generators.StructureGenerator
import smithy.kotlin.codegen.integration.KotlinIntegration
import smithy.kotlin.codegen.integration.ProtocolGenerator
import smithy.kotlin.codegen.utils.getLogger
import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.neighbor.Walker
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeVisitor
import software.amazon.smithy.model.shapes.StructureShape
import java.util.ServiceLoader
import java.util.TreeSet

class CodegenVisitor(context: PluginContext) : ShapeVisitor.Default<Unit>() {
    companion object {
        private val LOGGER = getLogger<CodegenVisitor>()
    }

    private val settings = KotlinSettings.configure(context.model, context.settings)
    private val nonTraits = context.modelWithoutTraitShapes
    private val model = context.model
    private val service = settings.getService(model)
    private val fileManifest = context.fileManifest

    private val symbolProvider: SymbolProvider
    private val protocolGenerator: ProtocolGenerator?
    private val applicationProtocol: ApplicationProtocol

    private val integrations: List<KotlinIntegration>

    private val sdkWriter = SdkWriter(fileManifest)

    init {
        LOGGER.info { "Generating Kotlin client for service ${service.id}" }

        // Load all integrations.
        val loader = context.pluginClassLoader.orElse(javaClass.classLoader)
        LOGGER.info { "Attempting to discover KotlinIntegrations from the classpath..." }
        integrations = ServiceLoader.load(KotlinIntegration::class.java, loader).onEach {
            LOGGER.info { "Adding KotlinIntegrations: $it" }
        }.toList()

        // Decorate the symbol provider using integrations.
        var resolvedProvider: SymbolProvider = SymbolVisitor(model)
        integrations.forEach {
            resolvedProvider = it.decorateSymbolProvider(settings, model, resolvedProvider)
        }
        symbolProvider = SymbolProvider.cache(resolvedProvider)

        // Resolve the nullable protocol generator and application protocol.
        protocolGenerator = resolveProtocolGenerator(integrations, service, settings)

        applicationProtocol = protocolGenerator?.applicationProtocol
            ?: ApplicationProtocol.createDefaultHttpApplicationProtocol()
    }

    private fun resolveProtocolGenerator(
        integrations: Collection<KotlinIntegration>,
        service: ServiceShape,
        settings: KotlinSettings
    ): ProtocolGenerator? {
        // Collect all of the supported protocol generators.
        val generators = integrations.flatMap { it.protocolGenerators }.associateBy { it.name }

        val protocolName = try {
            settings.resolveServiceProtocol(service, generators.keys)
        } catch (e: UnresolvableProtocolException) {
            LOGGER.warning { "Unable to find a protocol generator for ${service.id}: ${e.message}" }
            null
        }

        return protocolName?.let { generators[protocolName] }
    }

    fun execute() {
        // Generate models that are connected to the service being generated.
        LOGGER.fine { "Walking shapes from ${service.id} to find shapes to generate" }
        val serviceShapes = TreeSet(Walker(nonTraits).walkShapes(service))

        // Condense duplicate shapes
        val shapeMap = condenseShapes(serviceShapes)

        // Generate models from condensed shapes
        shapeMap.values.forEach {
            it.accept(this)
        }

        sdkWriter.writeFiles()
    }

    private fun condenseShapes(shapes: Set<Shape>): Map<String, Shape> {
        val shapeMap = mutableMapOf<String, Shape>()

        // Check for colliding shapes and prune non-unique shapes
        shapes.forEach { shape ->
            val shapeReference = "${shape.type}${shape.id.asRelativeReference()}"
            if (shapeMap.containsKey(shapeReference)) {
                val knownShape = shapeMap.getValue(shapeReference)
                if (isShapeCollision(shape, knownShape)) {
                    throw CodegenException("Shape Collision: cannot condense $shape and $knownShape")
                }
            } else {
                shapeMap[shapeReference] = shape
            }
        }

        return shapeMap
    }

    private fun isShapeCollision(shapeA: Shape, shapeB: Shape): Boolean {
        // Check names match.
        if (shapeA.id.name != shapeB.id.name) {
            return true
        }

        // Check traits match.
        if (shapeA.allTraits != shapeB.allTraits) {
            return false
        }

        // Check members match.
        val memberShapesA = shapeA.members()
        val memberShapesB = shapeB.members()
        memberShapesA.forEach { memberShape ->
            if (memberShapesB.none { it.memberName.contains(memberShape.memberName) }) {
                return true
            }
        }
        memberShapesB.forEach { otherMemberShape ->
            if (memberShapesA.none { it.memberName.contains(otherMemberShape.memberName) }) {
                return true
            }
        }
        return false
    }

    override fun serviceShape(shape: ServiceShape) {
        if (service != shape) {
            LOGGER.fine { "Skipping `${service.id}` because it is not `${service.id}`" }
            return
        }

        val serviceSymbol = symbolProvider.toSymbol(shape)
        val clientInterfaceName = serviceSymbol.name.removeSuffix("Client")
        val fileName = "$clientInterfaceName.kt"

        sdkWriter.useFile(fileName) {
            ServiceDefinitionGenerator(settings, model, symbolProvider, it).run()
        }
    }

    override fun structureShape(shape: StructureShape) {
        val structureSymbol = symbolProvider.toSymbol(shape)

        sdkWriter.useFile(structureSymbol.definitionFile) {
            StructureGenerator(model, symbolProvider, shape, it).generate()
        }
    }

    override fun getDefault(shape: Shape) {
        println("Unsupported shape $shape")
    }
}
