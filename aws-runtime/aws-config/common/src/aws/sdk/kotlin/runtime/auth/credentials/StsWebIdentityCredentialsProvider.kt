/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.crt.auth.credentials.build
import aws.sdk.kotlin.runtime.crt.SdkDefaultIO
import aws.sdk.kotlin.crt.auth.credentials.StsWebIdentityCredentialsProvider as StsWebIdentityCredentialsProviderCrt

/**
 * A provider that gets credentials from the STS web identity credential provider.
 */
public class StsWebIdentityCredentialsProvider : CrtCredentialsProvider {
    override val crtProvider: StsWebIdentityCredentialsProviderCrt = StsWebIdentityCredentialsProviderCrt.build {
        clientBootstrap = SdkDefaultIO.ClientBootstrap
        tlsContext = SdkDefaultIO.TlsContext
    }
}
