/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileCredentialsProviderTest {
    companion object {
        private const val ID = "abcd"
        private const val KEY = "efgh"
        private val EXPECTED_CREDS = Credentials(ID, KEY, null)
    }

    @Test
    fun `it should create a provider for a valid profile`() = runBlocking {
        val credsPath = Files.createTempFile("ProfileCredentialsProviderTest_creds", "")
        credsPath.toFile().writeText(
            """
                [default]
                aws_access_key_id = $ID
                aws_secret_access_key = $KEY
            """.trimIndent()
        )

        try {
            val provider = ProfileCredentialsProvider.Builder().run {
                credentialsFileNameOverride = credsPath.toString()
                build()
            }
            val actual = provider.getCredentials()
            assertEquals(EXPECTED_CREDS, actual)
        } finally {
            Files.deleteIfExists(credsPath)
        }
    }
}
