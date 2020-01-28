//package smithy.kotlin.codegen.generators
//
//import org.junit.jupiter.api.Test
//import smithy.kotlin.codegen.KotlinCodegenPlugin
//import software.amazon.smithy.model.Model
//import software.amazon.smithy.model.shapes.StringShape
//import software.amazon.smithy.model.traits.EnumConstantBody
//import software.amazon.smithy.model.traits.EnumTrait
//
//internal class EnumGeneratorTest {
//    @Test
//    fun generatesNamedEnums() {
//        val trait = EnumTrait.builder()
//            .addEnum("FOO", EnumConstantBody.builder().name("FOO").build())
//            .addEnum("BAR", EnumConstantBody.builder().name("BAR").build())
//            .build()
//        val shape = StringShape.builder().id("com.foo#Baz").addTrait(trait).build()
//        val symbol = KotlinCodegenPlugin.createProvider(Model.builder().build()).toSymbol(shape)
//
//        val run = EnumGenerator(shape, symbol).run()
//        println(run)
////
////        assertThat(writer.toString(), containsString("export enum Baz {"))
////        assertThat(writer.toString(), containsString("FOO = \"FOO\""))
////        assertThat(writer.toString(), containsString("BAR = \"BAR\","))
//    }
//
//    @Test
//    fun generatesUnnamedEnums() {
////        val trait = EnumTrait.builder()
////            .addEnum("FOO", EnumConstantBody.builder().build())
////            .addEnum("BAR", EnumConstantBody.builder().build())
////            .build()
////        val shape = StringShape.builder().id("com.foo#Baz").addTrait(trait).build()
////        val writer = TypeScriptWriter("foo")
////        val symbol: Symbol = TypeScriptCodegenPlugin.createSymbolProvider(
////            Model.builder().build()
////        ).toSymbol(shape)
////        EnumGenerator(shape, symbol, writer).run()
////        assertThat(writer.toString(), containsString("export type Baz = \"BAR\" | \"FOO\""))
//    }
//}