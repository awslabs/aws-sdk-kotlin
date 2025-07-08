/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.kotlin.codegen.test.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnvironmentTokenCustomizationTest {
    private val bedrockModel = """
        namespace com.test
        use aws.auth#sigv4
        use aws.api#service

        @sigv4(name: "bedrock")
        @service(sdkId: "Bedrock")
        service Bedrock {
            version: "1.0.0"
        }
    """.trimIndent().toSmithyModel()

    private val nonBedrockModel = """
        namespace com.test
        use aws.auth#sigv4
        use aws.api#service

        @sigv4(name: "s3")
        @service(sdkId: "S3")
        service S3 {
            version: "1.0.0"
        }
    """.trimIndent().toSmithyModel()

    private val noSigV4Model = """
        namespace com.test
        use aws.api#service

        @service(sdkId: "NoSigV4")
        service NoSigV4 {
            version: "1.0.0"
        }
    """.trimIndent().toSmithyModel()

    @Test
    fun `test customization enabled for bedrock sigv4 signing name`() {
        assertTrue {
            EnvironmentTokenCustomization()
                .enabledForService(bedrockModel, bedrockModel.defaultSettings())
        }
    }

    @Test
    fun `test customization not enabled for non-bedrock sigv4 signing name`() {
        assertFalse {
            EnvironmentTokenCustomization()
                .enabledForService(nonBedrockModel, nonBedrockModel.defaultSettings())
        }
    }

    @Test
    fun `test customization not enabled for model without sigv4 trait`() {
        assertFalse {
            EnvironmentTokenCustomization()
                .enabledForService(noSigV4Model, noSigV4Model.defaultSettings())
        }
    }
}
