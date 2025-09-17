/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.ktlintrules.minorversionstrategy

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import software.aws.ktlint.rules.deprecatedApiRule

/**
 * Ruleset provider for AWS SDK Kotlin minor-version-bump-specific Ktlint rules.
 */
class MinorVersionStrategyRuleSetProvider : RuleSetProviderV3(RuleSetId("minor-version-strategy-rules")) {
    private val sdkVersion = System.getProperty("sdkVersion").split(".")
    private val majorVersion = sdkVersion[0].toInt()
    private val minorVersion = sdkVersion[1].toInt()

    override fun getRuleProviders(): Set<RuleProvider> = setOf(
        RuleProvider {
            // Look for APIs that are scheduled for removal in upcoming minor version
            deprecatedApiRule(majorVersion, minorVersion + 1)
        },
    )
}
