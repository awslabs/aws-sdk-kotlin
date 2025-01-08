/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.dsql

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.net.Host
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class AuthTokenGeneratorTest {
    @Test
    fun testGenerateDbConnectAuthToken() = runTest {
        val credentials = Credentials("akid", "secret")

        val token = AuthTokenGenerator(credentials)
            .generateDbConnectAuthToken(
                endpoint = Url { host = Host.parse("peccy.dsql.us-east-1.on.aws") },
                region = "us-east-1",
                expiration = 450.seconds
            )

        // Token should have a parameter Action=DbConnect
        assertContains(token, "peccy.dsql.us-east-1.on.aws?Action=DbConnect")

        // Match the X-Amz-Credential parameter for any signing date
        val credentialRegex = Regex("X-Amz-Credential=akid%2F(\\d{8})%2Fus-east-1%2Fdsql%2Faws4_request")
        assertTrue(token.contains(credentialRegex))

        assertContains(token, "X-Amz-Expires=450")

        // Token should not contain a scheme
        listOf("http://", "https://").forEach {
            assertFalse(token.contains(it))
        }
    }

    @Test
    fun testGenerateDbConnectAuthAdminToken() = runTest {
        val credentials = Credentials("akid", "secret")

        val token = AuthTokenGenerator(credentials)
            .generateDbConnectAdminAuthToken(
                endpoint = Url { host = Host.parse("peccy.dsql.us-east-1.on.aws") },
                region = "us-east-1",
                expiration = 450.seconds
            )

        // Token should have a parameter Action=DbConnect
        assertContains(token, "peccy.dsql.us-east-1.on.aws?Action=DbConnectAdmin")

        // Match the X-Amz-Credential parameter for any signing date
        val credentialRegex = Regex("X-Amz-Credential=akid%2F(\\d{8})%2Fus-east-1%2Fdsql%2Faws4_request")
        assertTrue(token.contains(credentialRegex))

        assertContains(token, "X-Amz-Expires=450")

        // Token should not contain a scheme
        listOf("http://", "https://").forEach {
            assertFalse(token.contains(it))
        }
    }
}