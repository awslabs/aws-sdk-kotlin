package smithy.kotlin.codegen

import org.apache.logging.log4j.LogManager
import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.codegen.core.ReservedWordSymbolProvider
import software.amazon.smithy.codegen.core.ReservedWordSymbolProvider.Escaper
import software.amazon.smithy.codegen.core.ReservedWordsBuilder
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.codegen.core.Symbol.Builder
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.codegen.core.SymbolReference
import software.amazon.smithy.codegen.core.SymbolReference.ContextOption.DECLARE
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.OperationIndex
import software.amazon.smithy.model.shapes.BigDecimalShape
import software.amazon.smithy.model.shapes.BigIntegerShape
import software.amazon.smithy.model.shapes.BlobShape
import software.amazon.smithy.model.shapes.BooleanShape
import software.amazon.smithy.model.shapes.ByteShape
import software.amazon.smithy.model.shapes.DocumentShape
import software.amazon.smithy.model.shapes.DoubleShape
import software.amazon.smithy.model.shapes.FloatShape
import software.amazon.smithy.model.shapes.IntegerShape
import software.amazon.smithy.model.shapes.ListShape
import software.amazon.smithy.model.shapes.LongShape
import software.amazon.smithy.model.shapes.MapShape
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ResourceShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.SetShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeType
import software.amazon.smithy.model.shapes.ShapeType.OPERATION
import software.amazon.smithy.model.shapes.ShapeVisitor
import software.amazon.smithy.model.shapes.ShortShape
import software.amazon.smithy.model.shapes.StringShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.shapes.TimestampShape
import software.amazon.smithy.model.shapes.ToShapeId
import software.amazon.smithy.model.shapes.UnionShape
import software.amazon.smithy.model.traits.EnumTrait
import software.amazon.smithy.model.traits.StreamingTrait
import software.amazon.smithy.utils.StringUtils
import java.util.HashSet

/**
 * This class is responsible for type mapping and file/identifier formatting.
 *
 * Reserved words for Kotlin are automatically escaped so that they are
 * prefixed with "_". See "reserved-words.txt" for the list of words.
 */
internal class SymbolVisitor(private val model: Model) : SymbolProvider,
    ShapeVisitor<Symbol> {
    private val escaper: Escaper
    private val outputShapes = mutableSetOf<StructureShape>()

    init {
        // Load reserved words from a new-line delimited file.
        val reservedWords = ReservedWordsBuilder()
            .loadWords(SymbolVisitor::class.java.getResource("reserved-words.txt"))
            .build()
        escaper = ReservedWordSymbolProvider.builder()
            // Only escape words when the symbol has a definition file to
            // prevent escaping intentional references to built-in types.
            .nameReservedWords(reservedWords)
            .escapePredicate { _: Shape, symbol: Symbol ->
                !StringUtils.isEmpty(symbol.definitionFile)
            }
            .buildEscaper()

        // Get each structure that's used as output or errors.
        val operationIndex = model.getKnowledge(OperationIndex::class.java)
        model.shapes(OperationShape::class.java)
            .forEach { operationShape: OperationShape? ->
                operationIndex.getOutput(operationShape).ifPresent { e: StructureShape -> outputShapes.add(e) }
                outputShapes.addAll(operationIndex.getErrors(operationShape))
            }
    }


    override fun toSymbol(shape: Shape): Symbol {
        val symbol = shape.accept(this)
        LOGGER.trace("Creating symbol from $shape: $symbol")
        return escaper.escapeSymbol(shape, symbol)
    }

    override fun toMemberName(shape: MemberShape): String {
        return escaper.escapeMemberName(shape.memberName)
    }

    override fun blobShape(shape: BlobShape): Symbol {
        return if (!shape.hasTrait(StreamingTrait::class.java)) {
            createSymbolBuilder(shape, "Uint8Array").build()
        } else createSymbolBuilder(shape, "ArrayBuffer | ArrayBufferView | string | Readable | Blob", null)
            .addReference(
                Symbol.builder().name("Readable").namespace("stream", "/").build()
            )
            .build()

        // Note: `Readable` needs an import and a dependency.
    }

    override fun booleanShape(shape: BooleanShape): Symbol {
        return createSymbolBuilder(shape, "boolean").build()
    }

    override fun listShape(shape: ListShape): Symbol {
        val reference = toSymbol(shape.member)
        return createSymbolBuilder(shape, String.format("Array<%s>", reference.name), null)
            .addReference(reference)
            .build()
    }

    override fun setShape(shape: SetShape): Symbol {
        val reference = toSymbol(shape.member)
        return createSymbolBuilder(shape, String.format("Set<%s>", reference.name), null)
            .addReference(reference)
            .build()
    }

    /**
     * Maps get generated as an inline interface with a fixed value type.
     *
     *
     * For example:
     *
     * <pre>`interface MyStructureShape {
     * memberPointingToMap: {[key: string]: string};
     * }
    `</pre> *
     *
     * @inheritDoc
     */
    override fun mapShape(shape: MapShape): Symbol {
        val reference = toSymbol(shape.value)
        return createSymbolBuilder(shape, String.format("{ [key: string]: %s }", reference.name), null)
            .addReference(reference)
            .build()
    }

    override fun byteShape(shape: ByteShape): Symbol {
        return createNumber(shape)
    }

    override fun shortShape(shape: ShortShape): Symbol {
        return createNumber(shape)
    }

    override fun integerShape(shape: IntegerShape): Symbol {
        return createNumber(shape)
    }

    override fun longShape(shape: LongShape): Symbol {
        return createNumber(shape)
    }

    override fun floatShape(shape: FloatShape): Symbol {
        return createNumber(shape)
    }

    override fun doubleShape(shape: DoubleShape): Symbol {
        return createNumber(shape)
    }

    private fun createNumber(shape: Shape): Symbol {
        return createSymbolBuilder(shape, "number").build()
    }

    override fun bigIntegerShape(shape: BigIntegerShape): Symbol {
        // BigInt is not supported across all environments, use big.js instead.
        return createBigJsSymbol(shape)
    }

    override fun bigDecimalShape(shape: BigDecimalShape): Symbol {
        return createBigJsSymbol(shape)
    }

    private fun createBigJsSymbol(shape: Shape): Symbol {
        TODO()
//        return createSymbolBuilder(shape, "Big", TypeScriptDependency.TYPES_BIG_JS.packageName)
//            .addDependency(TypeScriptDependency.TYPES_BIG_JS)
//            .addDependency(TypeScriptDependency.BIG_JS)
//            .build()
    }

    override fun documentShape(shape: DocumentShape): Symbol {
        return addSmithyImport(createSymbolBuilder(shape, "_smithy.DocumentType.Value")).build()
    }

    override fun operationShape(shape: OperationShape): Symbol {
        val commandName = flattenShapeName(shape) + "Command"
        val moduleName = formatModuleName(shape.type, commandName)
        val intermediate =
            createGeneratedSymbolBuilder(shape, commandName, moduleName).build()
        val builder = intermediate.toBuilder()
        // Add input and output type symbols (XCommandInput / XCommandOutput).
        builder.putProperty("inputType", intermediate.toBuilder().name(commandName + "Input").build())
        builder.putProperty("outputType", intermediate.toBuilder().name(commandName + "Output").build())
        return builder.build()
    }

    override fun stringShape(shape: StringShape): Symbol {
        // Enums that provide a name for each variant create an actual enum type.
        return shape.getTrait(EnumTrait::class.java)
            .map { enumTrait: EnumTrait ->
                createEnumSymbol(
                    shape,
                    enumTrait
                )
            }
            .orElseGet {
                createSymbolBuilder(
                    shape,
                    "string"
                ).build()
            }
    }

    private fun createEnumSymbol(shape: StringShape, enumTrait: EnumTrait): Symbol {
        return createObjectSymbolBuilder(shape)
            .putProperty(EnumTrait::class.java.name, enumTrait)
            .build()
    }

    override fun resourceShape(shape: ResourceShape): Symbol {
        return createObjectSymbolBuilder(shape).build()
    }

    override fun serviceShape(shape: ServiceShape): Symbol {
        val name =
            StringUtils.capitalize(shape.id.name) + "Client"
        val moduleName = formatModuleName(shape.type, name)
        return createGeneratedSymbolBuilder(shape, name, moduleName).build()
    }

    override fun structureShape(shape: StructureShape): Symbol {
        val builder = createObjectSymbolBuilder(shape)
        addSmithyImport(builder)
        if (outputShapes.contains(shape)) {
            val reference = SymbolReference.builder()
                .options(DECLARE)
                .alias("\$MetadataBearer")
//                .symbol(TypeScriptDependency.AWS_SDK_TYPES.createSymbol("MetadataBearer"))
                .putProperty(IMPLEMENTS_INTERFACE_PROPERTY, true)
                .build()
            builder.addReference(reference)
            builder.putProperty("isOutput", true)
        }
        return builder.build()
    }

    private fun addSmithyImport(builder: Builder): Builder {
        val importSymbol =
            Symbol.builder()
                .name("*")
                .namespace("@aws-sdk/smithy-client", "/")
                .build()
        val reference = SymbolReference.builder()
            .symbol(importSymbol)
            .alias("_smithy")
            .options(DECLARE)
            .build()
        return builder.addReference(reference)
    }

    override fun unionShape(shape: UnionShape): Symbol {
        return createObjectSymbolBuilder(shape).build()
    }

    override fun memberShape(shape: MemberShape): Symbol {
        val targetShape = model.getShape(shape.target)
            .orElseThrow { CodegenException("Shape not found: " + shape.target) }
        val targetSymbol = toSymbol(targetShape)
        return if (targetSymbol.properties.containsKey(EnumTrait::class.java.name)) {
            createMemberSymbolWithEnumTarget(targetSymbol)
        } else targetSymbol
    }

    private fun createMemberSymbolWithEnumTarget(targetSymbol: Symbol): Symbol {
        return targetSymbol.toBuilder()
            .namespace(null, "/")
            .name(targetSymbol.name + " | string")
            .addReference(targetSymbol)
            .build()
    }

    override fun timestampShape(shape: TimestampShape): Symbol {
        return createSymbolBuilder(shape, "Date").build()
    }

    private fun flattenShapeName(id: ToShapeId): String {
        return StringUtils.capitalize(id.toShapeId().name)
    }

    private fun createObjectSymbolBuilder(shape: Shape): Builder {
        val name = flattenShapeName(shape)
        val moduleName = formatModuleName(shape.type, name)
        return createGeneratedSymbolBuilder(shape, name, moduleName)
    }

    private fun createSymbolBuilder(
        shape: Shape,
        typeName: String
    ): Builder {
        return Symbol.builder().putProperty("shape", shape).name(typeName)
    }

    private fun createSymbolBuilder(
        shape: Shape,
        typeName: String,
        namespace: String?
    ): Builder {
        return Symbol.builder()
            .putProperty("shape", shape)
            .name(typeName)
            .namespace(namespace, "/")
    }

    private fun createGeneratedSymbolBuilder(
        shape: Shape,
        typeName: String,
        namespace: String
    ): Builder {
        return createSymbolBuilder(shape, typeName, namespace)
            .definitionFile(toFilename(namespace))
    }

    private fun formatModuleName(shapeType: ShapeType, name: String): String {
        // All shapes except for the service and operations are stored in models.
        return if (shapeType == ShapeType.SERVICE) {
            "./$name"
        } else if (shapeType == OPERATION) {
            "./commands/$name"
        } else {
            "./models/index"
        }
    }

    private fun toFilename(namespace: String): String {
        return "$namespace.kt"
    }

    companion object {
        const val IMPLEMENTS_INTERFACE_PROPERTY = "implementsInterface"
        private val LOGGER = LogManager.getLogger(SymbolVisitor::class)
    }
}