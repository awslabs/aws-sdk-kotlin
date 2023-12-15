/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.dokka.transformers

import org.jetbrains.dokka.base.renderers.html.SearchRecord
import org.jetbrains.dokka.base.renderers.html.SearchbarDataInstaller
import org.jetbrains.dokka.pages.DriResolver
import org.jetbrains.dokka.plugability.DokkaContext

/**
 * Disable the search bar data (pages.json).
 */
class NoOpSearchbarDataInstaller(context: DokkaContext) : SearchbarDataInstaller(context) {
    override fun generatePagesList(pages: List<SignatureWithId>, locationResolver: DriResolver): List<SearchRecord> = listOf()
}
