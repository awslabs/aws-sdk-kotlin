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
         @title("Test Service")
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
    }

    @Test
    fun rendersBoilerplate() =
        ModuleDocumentationIntegration(
            codeExamples = mapOf("Test" to "https://example.com"),
        )
            .generateModuleDocumentation(ctx.toGenerationContext())
            .shouldContainOnlyOnceWithDiff(
                """
                    # Module test
                    
                    Test Service
                """.trimIndent(),
            )

    @Test
    fun rendersCodeExampleDocs() =
        ModuleDocumentationIntegration(
            codeExamples = mapOf("Test" to "https://example.com"),
        )
            .generateModuleDocumentation(ctx.toGenerationContext())
            .shouldContainOnlyOnceWithDiff(
                """
                    ## Code Examples
                    To see full code examples, see the Test Service examples in the AWS code example library. See https://example.com
                """.trimIndent(),
            )
}
