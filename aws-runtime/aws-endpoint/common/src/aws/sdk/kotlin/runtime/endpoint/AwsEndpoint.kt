/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.endpoint

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.http.Url
import aws.smithy.kotlin.runtime.http.endpoints.Endpoint
import aws.smithy.kotlin.runtime.util.AttributeKey

/**
 * Represents the endpoint a service client should make API operation calls to.
 *
 * The SDK will automatically resolve these endpoints per API client using an internal resolver.
 *
 * @property endpoint The endpoint clients will use to make API calls
 * to e.g. "{service-id}.{region}.amazonaws.com"
 * @property credentialScope Custom signing constraint overrides
 */
// TODO: remove when endpoints2 is fully implemented
public data class AwsEndpoint(
    public val endpoint: Endpoint,
    public val credentialScope: CredentialScope? = null,
) {
    public constructor(url: Url, credentialScope: CredentialScope? = null) : this(Endpoint(url), credentialScope)
    public constructor(url: String, credentialScope: CredentialScope? = null) : this(Endpoint(Url.parse(url)), credentialScope)
}

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
