/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.regions.providers

import software.aws.clientrt.logging.Logger

/**
 * Composite [AwsRegionProvider] that delegates to a chain of providers.
 * [providers] are consulted in the order given and the first region found is returned
 *
 * @param providers the list of providers to delegate to
 */
public open class AwsRegionProviderChain(
    private vararg val providers: AwsRegionProvider
) : AwsRegionProvider {
    private val logger = Logger.getLogger<AwsRegionProviderChain>()

    override fun toString(): String = buildString {
        append("AwsRegionProviderChain")
        providers.fold(this) { sb, provider ->
            sb.append(" -> ")
            sb.append(provider::class.simpleName)
        }
    }

    override suspend fun getRegion(): String? {
        // FIXME - 1.5 had firstNotNullOfOrNull()

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
