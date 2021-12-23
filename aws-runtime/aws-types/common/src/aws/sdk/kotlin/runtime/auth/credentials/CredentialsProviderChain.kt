/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.logging.Logger

/**
 * Composite [CredentialsProvider] that delegates to a chain of providers. When asked for credentials [providers]
 * are consulted in the order given until one succeeds. If none of the providers in the chain can provide credentials
 * then this class will throw an exception. The exception will include the providers tried in the message. Each
 * individual exception is available as a suppressed exception.
 *
 * @param providers the list of providers to delegate to
 */
public open class CredentialsProviderChain(
    protected vararg val providers: CredentialsProvider
) : CredentialsProvider, Closeable {
    private val logger = Logger.getLogger<CredentialsProviderChain>()

    init {
        require(providers.isNotEmpty()) { "at least one provider must be in the chain" }
    }

    override fun toString(): String =
        (listOf(this) + providers).map { it::class.simpleName }.joinToString(" -> ")

    override suspend fun getCredentials(): Credentials {
        // FIXME - this should be a CredentialsProviderException
        val chainException = lazy { ClientException("No credentials could be loaded from the chain: $this") }
        for (provider in providers) {
            try {
                return provider.getCredentials()
            } catch (ex: Exception) {
                logger.debug { "unable to load credentials from $provider: ${ex.message}" }
                chainException.value.addSuppressed(ex)
            }
        }

        throw chainException.value
    }

    override fun close() {
        providers.forEach { (it as? Closeable)?.close() }
    }
}
