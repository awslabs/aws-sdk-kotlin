/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.crt.auth.credentials.CredentialsProvider as CredentialsProviderCrt

internal interface CrtCredentialsProvider : CredentialsProvider {
    val crtProvider: CredentialsProviderCrt

    override suspend fun getCredentials(): Credentials = crtProvider.getCredentials().toSdk()
}
