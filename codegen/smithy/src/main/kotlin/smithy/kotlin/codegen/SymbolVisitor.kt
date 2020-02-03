package smithy.kotlin.codegen

import com.soywiz.klock.DateTime
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import smithy.kotlin.codegen.utils.getLogger
import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.codegen.core.ReservedWordSymbolProvider
import software.amazon.smithy.codegen.core.ReservedWordSymbolProvider.Escaper
import software.amazon.smithy.codegen.core.ReservedWordsBuilder
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.codegen.core.Symbol.Builder
import software.amazon.smithy.codegen.core.SymbolProvider
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
        LOGGER.info { "Creating symbol from $shape: $symbol" }
        return escaper.escapeSymbol(shape, symbol)
    }

    override fun toMemberName(shape: MemberShape): String {
        return escaper.escapeMemberName(shape.memberName)
    }

    override fun blobShape(shape: BlobShape): Symbol {
        return if (!shape.hasTrait(StreamingTrait::class.java)) {
            createSymbolBuilder(shape, BYTE_ARRAY).build()
        } else {
            TODO()
        }
    }

    override fun booleanShape(shape: BooleanShape): Symbol {
        return createSymbolBuilder(shape, BOOLEAN).build()
    }

    override fun listShape(shape: ListShape): Symbol {
        val reference = toSymbol(shape.member)

        return createSymbolBuilder(shape, LIST)
            .addReference(reference)
            .build()
    }

    override fun setShape(shape: SetShape): Symbol {
        val reference = toSymbol(shape.member)

        return createSymbolBuilder(shape, SET)
            .addReference(reference)
            .build()
    }

    override fun mapShape(shape: MapShape): Symbol {
        val keyReference = toSymbol(shape.key)
        val valueReference = toSymbol(shape.value)

        return createSymbolBuilder(shape, MAP)
            .addReference(keyReference)
            .addReference(valueReference)
            .build()
    }

    override fun byteShape(shape: ByteShape): Symbol {
        return createSymbolBuilder(shape, BYTE).build()
    }

    override fun shortShape(shape: ShortShape): Symbol {
        return createSymbolBuilder(shape, SHORT).build()
    }

    override fun integerShape(shape: IntegerShape): Symbol {
        return createSymbolBuilder(shape, INT).build()
    }

    override fun longShape(shape: LongShape): Symbol {
        return createSymbolBuilder(shape, LONG).build()
    }

    override fun floatShape(shape: FloatShape): Symbol {
        return createSymbolBuilder(shape, FLOAT).build()
    }

    override fun doubleShape(shape: DoubleShape): Symbol {
        return createSymbolBuilder(shape, DOUBLE).build()
    }

    override fun bigIntegerShape(shape: BigIntegerShape): Symbol {
        TODO()
    }

    override fun bigDecimalShape(shape: BigDecimalShape): Symbol {
        TODO()
    }

    override fun documentShape(shape: DocumentShape): Symbol {
        TODO()
    }

    override fun operationShape(shape: OperationShape): Symbol {
        val operationName = flattenShapeName(shape)
//        val packageName = formatPackageName(shape.type, operationName)
        val intermediate = createSymbolBuilder(shape, ClassName("", operationName)).build()
        val builder = intermediate.toBuilder()
        // Add input and output type symbols (XCommandInput / XCommandOutput).
        builder.putProperty("inputType", intermediate.toBuilder().name(operationName + "Input").build())
        builder.putProperty("outputType", intermediate.toBuilder().name(operationName + "Output").build())
        return builder.build()
    }

    override fun stringShape(shape: StringShape): Symbol {
        // Enums that provide a name for each variant create an actual enum type.
        return shape.getTrait(EnumTrait::class.java)
            .map { enumTrait: EnumTrait ->
                createEnumSymbol(shape, enumTrait)
            }
            .orElseGet {
                createSymbolBuilder(shape, STRING).build()
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
        val name = StringUtils.capitalize(shape.id.name) + "Client"
        val packageName = formatPackageName(shape.type, name)
        return createGeneratedSymbolBuilder(shape, ClassName(packageName, name)).build()
    }

    override fun structureShape(shape: StructureShape): Symbol {
        return createObjectSymbolBuilder(shape).build()
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
        } else {
            targetSymbol
        }
    }

    private fun createMemberSymbolWithEnumTarget(targetSymbol: Symbol): Symbol {
        return targetSymbol.toBuilder()
            .namespace(null, "/")
            .name(targetSymbol.name + " | string")
            .addReference(targetSymbol)
            .build()
    }

    override fun timestampShape(shape: TimestampShape): Symbol {
        return createSymbolBuilder(
            shape,
            ClassName(DateTime::class.java.packageName, DateTime::class.java.simpleName)
        ).build()
    }

    private fun flattenShapeName(id: ToShapeId): String {
        return StringUtils.capitalize(id.toShapeId().name)
    }

    private fun createObjectSymbolBuilder(shape: Shape): Builder {
        val name = flattenShapeName(shape)
        val packageName = formatPackageName(shape.type, name)
        return createGeneratedSymbolBuilder(shape, ClassName(packageName, name))
    }

    private fun createSymbolBuilder(shape: Shape, className: ClassName): Builder {
        val builder = Symbol.builder()
            .putProperty("shape", shape)
            .name(className.simpleName)

        if (className.packageName.isNotEmpty()) {
            builder.namespace(className.packageName, ".")
        }

        return builder
    }

    private fun createSymbolBuilder(shape: Shape, typeName: String, namespace: String): Builder {
        return Symbol.builder()
            .putProperty("shape", shape)
            .name(typeName)
            .namespace(namespace, ".")
    }

    private fun createGeneratedSymbolBuilder(shape: Shape, className: ClassName): Builder {
        return createSymbolBuilder(shape, className)
            .definitionFile(toFilename(className))
    }

    private fun formatPackageName(shapeType: ShapeType, name: String): String {
        // TODO: Package name prefix
        // All shapes except for the service and operations are stored in models.
        return when (shapeType) {
            ShapeType.SERVICE -> name
            else -> "models"
        }
    }

    private fun toFilename(className: ClassName): String {
        return "${className.packageName.replace('.', '/')}/${className.simpleName}.kt"
    }

    companion object {
        const val IMPLEMENTS_INTERFACE_PROPERTY = "implementsInterface"
        private val LOGGER = getLogger<SymbolVisitor>()
    }
}