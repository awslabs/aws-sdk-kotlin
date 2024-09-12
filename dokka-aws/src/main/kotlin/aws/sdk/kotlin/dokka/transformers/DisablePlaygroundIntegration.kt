/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.dokka.transformers

import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

@Suppress("UNUSED_PARAMETER") // `context` is required by the `provides` DSL method for installing transformers
class DisablePlaygroundIntegration(context: DokkaContext) : PageTransformer {
    override fun invoke(input: RootPageNode) = input.transformContentPagesTree { page ->
        page.modified(
            content = page.content,
            embeddedResources = page.embeddedResources.filterNot { "unpkg.com" in it },
        )
    }
}
