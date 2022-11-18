/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.endpoint

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningAttributes
import aws.smithy.kotlin.runtime.auth.awssigning.SigningContext
import aws.smithy.kotlin.runtime.http.endpoints.Endpoint
import aws.smithy.kotlin.runtime.http.operation.SdkHttpRequest
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
public val Endpoint.authScheme: AuthScheme.SigV4?
    get() = attributes.getOrNull(AuthSchemesAttributeKey)?.find { it is AuthScheme.SigV4 } as? AuthScheme.SigV4

@InternalSdkApi
public fun AuthScheme.SigV4.asSigningContext(): SigningContext = SigningContext(signingName, signingRegion)

/**
 * Update a request's signing context properties with the receiving auth scheme.
 */
@InternalSdkApi
public fun AuthScheme.SigV4.applyToRequest(req: SdkHttpRequest) {
    signingName?.let {
        if (it.isNotBlank()) req.context[AwsSigningAttributes.SigningService] = it
    }
    signingRegion?.let {
        if (it.isNotBlank()) req.context[AwsSigningAttributes.SigningRegion] = it
    }
}
