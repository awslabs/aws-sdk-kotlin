/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.test.newTestContext
import software.amazon.smithy.kotlin.codegen.test.prependNamespaceAndService
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.StructureShape
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveChecksumSelectionFieldsTest {
    @Test
    fun testRemovingChecksumFields() {
        val model = """
            @httpChecksum(
                requestValidationModeMember: "checksumMode"
            )
            operation GetObject {
                input: GetObjectRequest,
            }
            
            structure GetObjectRequest {
                key: String,
                checksumMode: ChecksumMode
            }
            
            @enum([
                {
                    value: "ENABLED",
                    name: "ENABLED"
                }
            ])
            string ChecksumMode
            
            @httpChecksum(
                requestAlgorithmMember: "checksumAlgorithm"
            )
            operation PutObject {
                input: PutObjectRequest,
            }
            
            structure PutObjectRequest {
                key: String,
                checksumAlgorithm: ChecksumAlgorithm
            }
            
            @enum([
                {
                    value: "CRC32C",
                    name: "CRC32C"
                },
                {
                    value: "CRC32",
                    name: "CRC32"
                },
                {
                    value: "SHA1",
                    name: "SHA1"
                },
                {
                    value: "SHA256",
                    name: "SHA256"
                }
            ])
            string ChecksumAlgorithm
        """.prependNamespaceAndService(
            imports = listOf("aws.protocols#httpChecksum"),
            operations = listOf("GetObject", "PutObject"),
        ).toSmithyModel()

        val ctx = model.newTestContext()
        val transformed = RemoveChecksumSelectionFields().preprocessModel(model, ctx.generationCtx.settings)

        val getOp = transformed.expectShape<OperationShape>("com.test#PutObject")
        val getReq = transformed.expectShape<StructureShape>(getOp.inputShape)
        assertTrue("key" in getReq.memberNames, "Expected 'key' in request object")
        assertFalse("checksumMode" in getReq.memberNames, "Unexpected 'checksumMode' in request object")

        val putOp = transformed.expectShape<OperationShape>("com.test#PutObject")
        val putReq = transformed.expectShape<StructureShape>(putOp.inputShape)
        assertTrue("key" in putReq.memberNames, "Expected 'key' in request object")
        assertFalse("checksumAlgorithm" in putReq.memberNames, "Unexpected 'checksumAlgorithm' in request object")
    }
}
