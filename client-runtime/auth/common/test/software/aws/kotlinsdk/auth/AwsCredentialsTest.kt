package software.aws.kotlinsdk.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class AwsCredentialsTest {

    @Test
    fun `it can iterate over providers until one matches`() {
        val unit: AwsCredentialsProviders = listOf(
            { null },
            { BasicAwsCredentials("access", "secret") },
            { BasicAwsCredentials("bad", "worse") }
        )

        val expected = BasicAwsCredentials("access", "secret")

        assertEquals(expected, unit.find())
    }
}
