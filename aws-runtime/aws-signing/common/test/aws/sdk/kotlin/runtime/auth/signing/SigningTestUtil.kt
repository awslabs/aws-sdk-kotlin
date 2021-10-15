/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.signing

import aws.sdk.kotlin.runtime.auth.credentials.Credentials
import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider

object TestCredentialsProvider : CredentialsProvider {
    val testCredentials = Credentials("AKID", "SECRET", "SESSION")
    override suspend fun getCredentials(): Credentials = testCredentials
}
