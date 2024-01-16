/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.aws.traits.auth.SigV4ATrait
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.AuthTrait
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SigV4AsymmetricTraitCustomizationTest {
    private val testModel = """
            namespace smithy.example

            use aws.protocols#awsJson1_0
            use aws.auth#sigv4

            @awsJson1_0
            @sigv4(name: "example-signing-name")
            service Example {
                version: "1.0.0",
                operations: [GetFoo]
            }

            operation GetFoo {
                input: GetFooInput
            }
            
            operation GetNotFoo {
                input: GetFooInput
            }
            
            structure GetFooInput {
                payload: String
            }            
        """.toSmithyModel()

    @Test
    fun testCustomizationAppliedCorrectly() {
        val customizedModel = SigV4AsymmetricTraitCustomization()
            .preprocessModel(
                testModel,
                KotlinSettings(
                    ShapeId.from("smithy.example#Example"),
                    KotlinSettings.PackageSettings("example", "1.0.0"),
                    "example",
                ),
            )

        assertTrue(customizedModel.appliedTraits.contains(SigV4ATrait.ID))
        assertTrue(customizedModel.appliedTraits.contains(AuthTrait.ID))

        val service = customizedModel.getShape(customizedModel.serviceShapes.first().id).get()
        val sigV4ATrait = service.getTrait<SigV4ATrait>() as SigV4ATrait
        val authTrait = service.getTrait<AuthTrait>() as AuthTrait

        assertEquals("example-signing-name", sigV4ATrait.name)
        assertTrue(authTrait.valueSet.contains(SigV4Trait.ID))
        assertTrue(authTrait.valueSet.contains(SigV4ATrait.ID))
    }
}
