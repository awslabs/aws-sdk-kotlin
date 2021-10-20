/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.crt.auth.credentials.Credentials as CredentialsCrt
import aws.sdk.kotlin.crt.auth.credentials.CredentialsProvider as CredentialsProviderCrt

/**
 * Proxy an instance of the Kotlin SDK version of [CredentialsProvider] as a CRT provider
 */
internal class CredentialsProviderCrtProxy(private val provider: CredentialsProvider) : CredentialsProviderCrt {
    override suspend fun getCredentials(): CredentialsCrt = provider.getCredentials().toCrt()
    override fun close() {}
    override suspend fun waitForShutdown() {}
}

/**
 * Convert Kotlin SDK credentials into CRT equivalent
 */
internal fun Credentials.toCrt(): CredentialsCrt = CredentialsCrt(accessKeyId, secretAccessKey, sessionToken)

/**
 * Convert CRT credentials into SDK equivalent
 */
internal fun CredentialsCrt.toSdk(): Credentials = Credentials(accessKeyId, secretAccessKey, sessionToken)

/**
 * Adapt or convert a SDK credentials provider into CRT equivalent
 */
internal fun asCrt(sdkProvider: CredentialsProvider): CredentialsProviderCrt = when (sdkProvider) {
    is CrtCredentialsProvider -> sdkProvider.crtProvider
    else -> CredentialsProviderCrtProxy(sdkProvider)
}
