package smithy.kotlin.codegen.generators

import smithy.kotlin.codegen.KotlinWriter
import smithy.kotlin.codegen.SymbolVisitor
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.StructureShape
import kotlin.test.Test

class StructureGeneratorTest {
//    @Test
//    fun properlyGeneratesEmptyMessageMemberOfException() {
//        testErrorStructureCodegen(
//            "error-test-empty.smithy",
//            "export interface Err extends _smithy.SmithyException, \$MetadataBearer {\n"
//                + "  __type: \"Err\";\n"
//                + "  name: \"Err\";\n"
//                + "  \$fault: \"client\";\n"
//                + "}"
//        )
//    }
//
//    @Test
//    fun properlyGeneratesOptionalMessageMemberOfException() {
//        testErrorStructureCodegen(
//            "error-test-optional-message.smithy",
//            "export interface Err extends _smithy.SmithyException, \$MetadataBearer {\n"
//                + "  __type: \"Err\";\n"
//                + "  name: \"Err\";\n"
//                + "  \$fault: \"client\";\n"
//                + "  message?: string;\n"
//                + "}"
//        )
//    }
//
//    @Test
//    fun properlyGeneratesRequiredMessageMemberOfException() {
//        testErrorStructureCodegen(
//            "error-test-required-message.smithy",
//            "export interface Err extends _smithy.SmithyException, \$MetadataBearer {\n"
//                + "  __type: \"Err\";\n"
//                + "  name: \"Err\";\n"
//                + "  \$fault: \"client\";\n"
//                + "  message: string | undefined;\n"
//                + "}"
//        )
//    }
//
//    fun testErrorStructureCodegen(file: String?, expectedType: String?) {
//        val model = Model.assembler()
//            .addImport(javaClass.getResource(file))
//            .assemble()
//            .unwrap()
//        val manifest = MockManifest()
//        val context = PluginContext.builder()
//            .model(model)
//            .fileManifest(manifest)
//            .settings(
//                Node.objectNodeBuilder()
//                    .withMember(
//                        "service",
//                        Node.from("smithy.example#Example")
//                    )
//                    .withMember(
//                        "package",
//                        Node.from("example")
//                    )
//                    .withMember(
//                        "packageVersion",
//                        Node.from("1.0.0")
//                    )
//                    .build()
//            )
//            .build()
//        TypeScriptCodegenPlugin().execute(context)
//        val contents = manifest.getFileString("/models/index.ts").get()
//        assertThat(contents, containsString(expectedType))
//        assertThat(
//            contents, containsString(
//                "namespace Err {\n"
//                    + "  export function isa(o: any): o is Err {\n"
//                    + "    return _smithy.isa(o, \"Err\");\n"
//                    + "  }\n"
//                    + "}"
//            )
//        )
//    }

    @Test
    fun generatesNonErrorStructures() {
        val struct = createNonErrorStructure()
        val assembler = Model.assembler().addShape(struct)
        struct.allMembers.values.forEach { assembler.addShape(it) }
        val model = assembler.assemble().unwrap()
        val writer = KotlinWriter("./Bar.kt")

        StructureGenerator(model, SymbolVisitor(model, settings), struct, writer).generate()

        val output = writer.toString()
        println(output)
    }

    @Test
    fun generatesNonErrorStructuresThatExtendOtherInterfaces() {
        val struct = createNonErrorStructure()
        val assembler = Model.assembler().addShape(struct)
        struct.allMembers.values.forEach { assembler.addShape(it) }
        val operation = OperationShape.builder().id("com.foo#Operation").output(struct).build()
        assembler.addShape(operation)
        val model = assembler.assemble().unwrap()
        val writer = KotlinWriter("./Foo.kt")

        StructureGenerator(model, SymbolVisitor(model, settings), struct, writer).generate()

        val output = writer.toString()
//        assertThat(output, containsString("export interface Bar extends \$MetadataBearer {"))
    }

    private fun createNonErrorStructure(): StructureShape {
        return StructureShape.builder()
            .id("com.foo#Bar")
            .addMember(MemberShape.builder().id("com.foo#Bar\$foo").target("smithy.api#String").build())
            .build()
    }
}