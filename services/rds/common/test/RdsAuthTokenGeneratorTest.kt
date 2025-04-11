/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.rds

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.net.Host
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.seconds

class RdsAuthTokenGeneratorTest {
    @Test
    fun testGenerateAuthToken() = runTest {
        val clock = ManualClock(Instant.fromEpochSeconds(1724716800))

        val credentials = Credentials("akid", "secret")
        val credentialsProvider = StaticCredentialsProvider(credentials)

        val generator = RdsAuthTokenGenerator(credentialsProvider, clock = clock)

        val token = generator.generateAuthToken(
            endpoint = Url {
                host = Host.parse("prod-instance.us-east-1.rds.amazonaws.com")
                port = 3306
            },
            region = "us-east-1",
            username = "peccy",
            expiration = 450.seconds,
        )

        // Token should have a parameter Action=connect, DBUser=peccy
        assertContains(token, "prod-instance.us-east-1.rds.amazonaws.com:3306?Action=connect&DBUser=peccy")
        assertContains(token, "X-Amz-Credential=akid%2F20240827%2Fus-east-1%2Frds-db%2Faws4_request")
        assertContains(token, "X-Amz-Expires=450")
        assertContains(token, "X-Amz-SignedHeaders=host")

        // Token should not contain a scheme
        listOf("http://", "https://").forEach {
            assertFalse(token.contains(it))
        }

        val urlToken = Url.parse("https://$token")
        val xAmzDate = urlToken.parameters.decodedParameters.getValue("X-Amz-Date").single()
        assertEquals(clock.now(), Instant.fromIso8601(xAmzDate))
    }
}
