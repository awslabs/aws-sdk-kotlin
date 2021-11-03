/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.dokka

import aws.sdk.kotlin.dokka.transformers.FilterInternalApis
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin

/**
 * Dokka plugin for customizing the AWS Kotlin SDK generated API docs
 */
class AwsDokkaPlugin : DokkaPlugin() {
    init {
        println("${this.javaClass.canonicalName} loaded!")
    }

    val dokkaBase by lazy { plugin<DokkaBase>() }

    val filterInternalApis by extending {
        dokkaBase.preMergeDocumentableTransformer providing ::FilterInternalApis
    }
}
