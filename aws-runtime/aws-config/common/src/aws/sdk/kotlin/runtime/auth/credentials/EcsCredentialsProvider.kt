/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.crt.auth.credentials.build
import aws.sdk.kotlin.runtime.crt.SdkDefaultIO
import aws.sdk.kotlin.crt.auth.credentials.EcsCredentialsProvider as EcsCredentialsProviderCrt

/**
 * A provider that gets credentials from an ECS environment
 *
 * @param host The host component of the URL to query credentials from
 * @param pathAndQuery The path and query components of the URI, concatenated, to query credentials from
 * @param authToken The token to pass to ECS credential service
 */
public class EcsCredentialsProvider public constructor(
    host: String? = null,
    pathAndQuery: String? = null,
    authToken: String? = null,
) : CrtCredentialsProvider {
    override val crtProvider: EcsCredentialsProviderCrt = EcsCredentialsProviderCrt.build {
        clientBootstrap = SdkDefaultIO.ClientBootstrap
        tlsContext = SdkDefaultIO.TlsContext
        this.host = host
        this.pathAndQuery = pathAndQuery
        this.authToken = authToken
    }
}
