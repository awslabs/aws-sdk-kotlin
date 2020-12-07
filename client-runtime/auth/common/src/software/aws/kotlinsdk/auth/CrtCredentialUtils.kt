/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.aws.kotlinsdk.auth

import software.amazon.awssdk.kotlin.crt.auth.credentials.Credentials as CredentialsCrt
import software.amazon.awssdk.kotlin.crt.auth.credentials.CredentialsProvider as CredentialsProviderCrt

/**
 * Proxy an instance of the Kotlin SDK version of [CredentialsProvider] as a CRT provider
 */
internal class CrtCredentialsProvider(private val provider: CredentialsProvider) : CredentialsProviderCrt {
    override suspend fun getCredentials(): CredentialsCrt {
        return provider.getCredentials().toCrt()
    }

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
