/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.dokka

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin

/**
 * Dokka plugin for customizing the AWS Kotlin SDK generated API docs
 */
class AwsDokkaPlugin : DokkaPlugin() {
    init {
        println("AwsDokkaPlugin loaded!")
    }

    val dokkaBase by lazy { plugin<DokkaBase>() }

    // val htmlExtension by extending {
    //     dokkaBase.htmlPreprocessors with AwsHtmlPreprocessor
    // }

    val filterInternalApis by extending {
        dokkaBase.preMergeDocumentableTransformer providing ::FilterInternalApis
    }
}

// object AwsHtmlPreprocessor : PageTransformer {
//     override fun invoke(input: RootPageNode): RootPageNode {
//         TODO("Not yet implemented")
//     }
// }

/**
 * Filters out anything annotated with `InternalSdkApi`
 */
class FilterInternalApis(context: DokkaContext) : SuppressedByConditionDocumentableFilterTransformer(context) {
    override fun shouldBeSuppressed(d: Documentable): Boolean {
        val isInternal = when (d) {
            is DClass -> d.isInternalSdk()
            is DObject -> d.isInternalSdk()
            is DTypeAlias -> d.isInternalSdk()
            is DFunction -> d.isInternalSdk()
            is DProperty -> d.isInternalSdk()
            is DEnum -> d.isInternalSdk()
            is DEnumEntry -> d.isInternalSdk()
            is DTypeParameter -> d.isInternalSdk()
            else -> false
        }
        if (isInternal) println("suppressing ${d.dri}")
        return isInternal
    }
}

fun <T> T.isInternalSdk() where T : WithExtraProperties<out Documentable> =
    internalAnnotation != null

val <T> T.internalAnnotation where T : WithExtraProperties<out Documentable>
    get() = extra[Annotations]?.let { annotations ->
        annotations.directAnnotations.values.flatten().firstOrNull {
            // aws.sdk.kotlin.runtime/InternalSdkApi///PointingToDeclaration/
            it.dri.toString() == "aws.sdk.kotlin.runtime/InternalSdkApi///PointingToDeclaration/"
        }
    }
