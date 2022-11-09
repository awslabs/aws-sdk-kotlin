/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.endpoint

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.auth.awssigning.SigningContext
import aws.smithy.kotlin.runtime.http.endpoints.Endpoint
import aws.smithy.kotlin.runtime.util.AttributeKey

/**
 * Static attribute key for AWS endpoint auth schemes.
 */
@InternalSdkApi
public val AuthSchemesAttributeKey: AttributeKey<List<AuthScheme>> = AttributeKey("authSchemes")

/**
 * A set of signing constraints for an AWS endpoint.
 */
@InternalSdkApi
public sealed class AuthScheme {
    public data class SigV4(
        public val signingName: String?,
        public val disableDoubleEncoding: Boolean,
        public val signingRegion: String?,
    ) : AuthScheme()

    public data class SigV4A(
        public val signingName: String?,
        public val disableDoubleEncoding: Boolean,
        public val signingRegionSet: List<String>,
    ) : AuthScheme()
}

/**
 * Sugar extension to pull an auth scheme out of the attribute set.
 *
 * FUTURE: Right now we only support sigv4. The auth scheme property can include multiple schemes, for now we only pull
 * out the sigv4 one if present.
 */
@InternalSdkApi
public fun Endpoint.authScheme(): AuthScheme.SigV4? =
    attributes.getOrNull(AuthSchemesAttributeKey)?.find { it is AuthScheme.SigV4 } as AuthScheme.SigV4?

@InternalSdkApi
public fun AuthScheme.SigV4.asSigningContext(): SigningContext = SigningContext(signingName, signingRegion)
