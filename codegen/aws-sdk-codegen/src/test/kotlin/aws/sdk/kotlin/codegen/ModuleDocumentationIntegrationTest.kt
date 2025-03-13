package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.test.newTestContext
import software.amazon.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff
import software.amazon.smithy.kotlin.codegen.test.toGenerationContext
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val model = """
         ${"$"}version: "2"
     
         namespace com.test
         
         use aws.api#service
     
         @service(sdkId: "Test")
         @title("A test service")
         service Test {
             version: "1.0.0",
             operations: []
         }
     """.toSmithyModel()

val ctx = model.newTestContext("Test")

class ModuleDocumentationIntegrationTest {
    @Test
    fun integrationIsAppliedCorrectly() {
        assertFalse(
            ModuleDocumentationIntegration().enabledForService(model, ctx.generationCtx.settings),
        )
        assertTrue(
            ModuleDocumentationIntegration(
                codeExamples = mapOf("Test" to "https://example.com"),
            ).enabledForService(model, ctx.generationCtx.settings),
        )
        assertTrue(
            ModuleDocumentationIntegration(
                handWritten = mapOf("Test" to "example.md"),
            ).enabledForService(model, ctx.generationCtx.settings),
        )
        assertTrue(
            ModuleDocumentationIntegration(
                codeExamples = mapOf("Test" to "https://example.com"),
                handWritten = mapOf("Test" to "test.md"),
            ).enabledForService(model, ctx.generationCtx.settings),
        )
    }

    @Test
    fun rendersBoilerplate() =
        ModuleDocumentationIntegration()
            .generateModuleDocumentation(
                ctx.toGenerationContext(),
                "Test",
            )
            .shouldContainOnlyOnceWithDiff(
                """
                    # Module test
                    
                    A test service
                """.trimIndent(),
            )

    @Test
    fun rendersCodeExampleDocs() =
        ModuleDocumentationIntegration(
            codeExamples = mapOf("Test" to "https://example.com"),
        )
            .generateModuleDocumentation(
                ctx.toGenerationContext(),
                "Test",
            )
            .shouldContainOnlyOnceWithDiff(
                """
                    ## Code Examples
                    To see full code examples, see the Test examples in the AWS Code Library. See https://example.com
                """.trimIndent(),
            )

    @Test
    fun rendersHandWrittenDocs() =
        ModuleDocumentationIntegration(
            handWritten = mapOf("Test" to "test.md"),
        )
            .generateModuleDocumentation(
                ctx.toGenerationContext(),
                "Test",
            ).replace("\r\n", "\n")  // Handle CRLF on Windows
            .shouldContainOnlyOnceWithDiff(
                """
                    ## Subtitle

                    Lorem Ipsum
                """.trimIndent(),
            )
}
