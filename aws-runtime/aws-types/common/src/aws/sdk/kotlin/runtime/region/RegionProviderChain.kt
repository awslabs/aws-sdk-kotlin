/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.region

import aws.smithy.kotlin.runtime.logging.Logger

/**
 * Composite [RegionProvider] that delegates to a chain of providers.
 * [providers] are consulted in the order given and the first region found is returned
 *
 * @param providers the list of providers to delegate to
 */
public open class RegionProviderChain(
    protected vararg val providers: RegionProvider
) : RegionProvider {
    private val logger = Logger.getLogger<RegionProviderChain>()

    init {
        require(providers.isNotEmpty()) { "at least one provider must be in the chain" }
    }

    override fun toString(): String =
        (listOf(this) + providers).map { it::class.simpleName }.joinToString(" -> ")

    override suspend fun getRegion(): String? {

        for (provider in providers) {
            try {
                val region = provider.getRegion()
                if (region != null) {
                    return region
                }
            } catch (ex: Exception) {
                logger.debug { "unable to load region from $provider: ${ex.message}" }
            }
        }

        return null
    }
}
