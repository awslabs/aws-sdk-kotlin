/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.retries.RetryConfigSection
import software.amazon.smithy.kotlin.codegen.retries.StandardRetryIntegration

private val policyLinePattern = Regex("""(\s*policy = )(.+)""")

/**
 * Adds AWS-specific retry wrappers around operation invocations. This reuses the [StandardRetryIntegration] but
 * replaces [StandardRetryPolicy][aws.smithy.kotlin.runtime.retries.impl] with
 * [AwsDefaultRetryPolicy][aws.sdk.kotlin.runtime.http.retries].
 */
class AwsDefaultRetryIntegration : KotlinIntegration {
    override val order: Byte
        get() = 10 // Must run after StandardRetryIntegration

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(rewriteConfig)

    private val rewriteConfig = SectionWriterBinding(RetryConfigSection) { writer, prev ->
        require(prev!!.contains(policyLinePattern)) { "Cannot find existing policy specification to override" }

        writer.addImport(AwsRuntimeTypes.Http.Retries.AwsDefaultRetryPolicy)

        prev
            .lineSequence()
            .map(::replacePolicy)
            .forEach(writer::write)
    }

    private fun replacePolicy(line: String): String =
        if (policyLinePattern.matches(line)) line.replace(policyLinePattern, "$1AwsDefaultRetryPolicy") else line
}
