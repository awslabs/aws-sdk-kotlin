/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.logging.Logger

/**
 * Composite [CredentialsProvider] that delegates to a chain of providers.
 * [providers] are consulted in the order given and the first region found is returned
 *
 * @param providers the list of providers to delegate to
 */
public open class CredentialsProviderChain(
    protected vararg val providers: CredentialsProvider
) : CredentialsProvider {
    private val logger = Logger.getLogger<CredentialsProviderChain>()

    init {
        require(providers.isNotEmpty()) { "at least one provider must be in the chain" }
    }

    override fun toString(): String =
        (listOf(this) + providers).map { it::class.simpleName }.joinToString(" -> ")

    override suspend fun getCredentials(): Credentials {
        for (provider in providers) {
            try {
                return provider.getCredentials()
            } catch (ex: Exception) {
                logger.debug { "unable to load credentials from $provider: ${ex.message}" }
            }
        }

        throw ClientException("No credentials could be loaded from the chain: $this")
    }
}
