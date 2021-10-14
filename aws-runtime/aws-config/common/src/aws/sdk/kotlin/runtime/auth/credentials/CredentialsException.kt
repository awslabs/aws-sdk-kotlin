/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.ClientException

public sealed class CredentialsError(public val message: String) {
    /**
     * No credentials were available from this provider
     */
    public class CredentialsNotLoaded(message: String?) : CredentialsError(message ?: "The provider could not provide credentials or required configuration was not set")

    /**
     * The provider was given an invalid configuration (e.g. invalid aws configuration file, invalid IMDS endpoint, etc)
     */
    public class InvalidConfiguration(message: String) : CredentialsError(message)

    /**
     * The provider experienced an error during credentials resolution
     */
    public class ProviderError(message: String) : CredentialsError(message)

    /**
     * An unexpected error occurred during credential resolution.
     * If the error is something that can occur during expected usage of a provider, [ProviderError] should be returned
     * instead. Unknown is for exceptional cases, for example data this is missing required fields.
     */
    public class Unknown(message: String) : CredentialsError(message)

    override fun toString(): String = "${this::class.simpleName}: $message"
}

/**
 * Exception base class for credential provider errors
 */
public open class CredentialsException(public val error: CredentialsError, cause: Throwable? = null) : ClientException(error.toString(), cause)

/**
 * wrapper function for constructing a [CredentialsException]
 */
internal fun throwCredentialsError(error: CredentialsError, cause: Throwable? = null): Nothing = throw CredentialsException(error, cause)
