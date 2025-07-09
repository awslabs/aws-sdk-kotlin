/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.kotlin.codegen.test.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnvironmentBearerTokenCustomizationTest {
    private val bedrockModel = """
        namespace com.test
        use aws.auth#sigv4
        use aws.api#service
        use smithy.api#httpBearerAuth
        
        @sigv4(name: "bedrock")
        @httpBearerAuth
        @service(sdkId: "Bedrock")
        service Bedrock {
            version: "1.0.0"
        }
    """.trimIndent().toSmithyModel()

    private val nonBedrockModel = """
        namespace com.test
        use aws.auth#sigv4
        use aws.api#service
        use smithy.api#httpBearerAuth
        
        @sigv4(name: "s3")
        @httpBearerAuth
        @service(sdkId: "S3")
        service S3 {
            version: "1.0.0"
        }
    """.trimIndent().toSmithyModel()

    private val noSigV4Model = """
        namespace com.test
        use aws.api#service
        use smithy.api#httpBearerAuth
        
        @service(sdkId: "NoSigV4")
        @httpBearerAuth
        service NoSigV4 {
            version: "1.0.0"
        }
    """.trimIndent().toSmithyModel()

    private val noBearerAuthModel = """
        namespace com.test
        use aws.auth#sigv4
        use aws.api#service

        @sigv4(name: "bedrock")
        @service(sdkId: "BedrockNoBearerAuth")
        service BedrockNoBearerAuth {
            version: "1.0.0"
        }
    """.trimIndent().toSmithyModel()

    @Test
    fun `test customization enabled for bedrock sigv4 signing name`() {
        assertTrue {
            EnvironmentBearerTokenCustomization()
                .enabledForService(bedrockModel, bedrockModel.defaultSettings())
        }
    }

    @Test
    fun `test customization not enabled for non-bedrock sigv4 signing name`() {
        assertFalse {
            EnvironmentBearerTokenCustomization()
                .enabledForService(nonBedrockModel, nonBedrockModel.defaultSettings())
        }
    }

    @Test
    fun `test customization not enabled for model without sigv4 trait`() {
        assertFalse {
            EnvironmentBearerTokenCustomization()
                .enabledForService(noSigV4Model, noSigV4Model.defaultSettings())
        }
    }

    @Test
    fun `test customization not enabled for model without bearer auth trait`() {
        assertFalse {
            EnvironmentBearerTokenCustomization()
                .enabledForService(noBearerAuthModel, noBearerAuthModel.defaultSettings())
        }
    }
}
