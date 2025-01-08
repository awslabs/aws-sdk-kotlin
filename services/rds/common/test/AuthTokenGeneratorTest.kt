/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.rds

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
            .generateAuthToken(
                endpoint = Url {
                    host = Host.parse("prod-instance.us-east-1.rds.amazonaws.com")
                    port = 3306
                },
                region = "us-east-1",
                username = "peccy",
                expiration = 450.seconds
            )

        // Token should have a parameter Action=DbConnect
        assertContains(token, "prod-instance.us-east-1.rds.amazonaws.com:3306?Action=connect&DBUser=peccy")

        // Match the X-Amz-Credential parameter for any signing date
        val credentialRegex = Regex("X-Amz-Credential=akid%2F(\\d{8})%2Fus-east-1%2Frds-db%2Faws4_request")
        assertTrue(token.contains(credentialRegex))

        assertContains(token, "X-Amz-Expires=450")

        // Token should not contain a scheme
        listOf("http://", "https://").forEach {
            assertFalse(token.contains(it))
        }
    }
}