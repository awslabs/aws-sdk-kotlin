package software.aws.kotlinsdk

import kotlin.test.Test
import kotlin.test.assertEquals

class AwsCredentialsTest {

    @Test
    fun `it can iterate over providers until one matches`() {
        val unit: AwsCredentialsProviders = listOf(
                { null },
                { AwsCredentials("access", "secret") },
                { AwsCredentials("bad", "worse") }
        )

        val expected = AwsCredentials("access", "secret")

        assertEquals(expected, unit.find())
    }
}