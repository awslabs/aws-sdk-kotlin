/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Operation
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.toHighLevel
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.rendering.HighLevelRenderer
import aws.sdk.kotlin.hll.codegen.util.Pkg
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * The high-level ops KSP processor. This class controls the overall flow of symbol processing, including resolving the
 * symbols to use as codegen input (i.e., the low-level DynamoDB operations/types), wiring up rendering context, and
 * starting the top-level renderer (which may in turn call other renderers).
 */
class HighLevelOpsProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val codeGenerator = environment.codeGenerator
    private var invoked = false
    private val logger = environment.logger
    private val opAllowlist = environment.options["op-allowlist"]?.split(";")
    private val pkg = environment.options["pkg"] ?: Pkg.Hl.Ops

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!invoked) {
            invoked = true

            logger.info("Scanning low-level DDB client for operations and types")
            val operations = getOperations(resolver)

            val codegenFactory = CodeGeneratorFactory(codeGenerator, logger)
            val ctx = RenderContext(logger, codegenFactory, pkg)

            HighLevelRenderer(ctx, operations).render()
        }

        return listOf()
    }

    private fun allow(func: KSFunctionDeclaration) =
        (opAllowlist?.contains(func.simpleName.getShortName()) ?: true).also {
            if (!it) logger.warn("${func.simpleName.getShortName()} not in allowlist; skipping codegen")
        }

    private fun getOperations(resolver: Resolver): List<Operation> = resolver
        .getClassDeclarationByName<DynamoDbClient>()!!
        .getDeclaredFunctions()
        .filter(::allow)
        .map(Operation::from)
        .map { it.toHighLevel(pkg) }
        .toList()
}
