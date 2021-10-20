/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.endpoint

import aws.smithy.kotlin.runtime.http.Url
import aws.smithy.kotlin.runtime.http.operation.Endpoint

/**
 * Represents the endpoint a service client should make API operation calls to.
 *
 * The SDK will automatically resolve these endpoints per API client using an internal resolver.
 *
 * @property endpoint The endpoint clients will use to make API calls
 * to e.g. "{service-id}.{region}.amazonaws.com"
 * @property credentialScope Custom signing constraint overrides
 */
public data class AwsEndpoint(
    public val endpoint: Endpoint,
    public val credentialScope: CredentialScope? = null
) {
    public constructor(url: Url, credentialScope: CredentialScope? = null) : this(Endpoint(url), credentialScope)
    public constructor(url: String, credentialScope: CredentialScope? = null) : this(Endpoint(Url.parse(url)), credentialScope)
}
