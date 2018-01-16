package software.amazon.awssdk.kotlin.codegen.plugin.testproject

import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import org.junit.Test
import software.amazon.awssdk.kotlin.services.polly.DefaultPollyClient
import software.amazon.awssdk.kotlin.services.polly.model.DescribeVoicesRequest
import software.amazon.awssdk.kotlin.services.polly.model.LanguageCode

class BasicCodeGenerationIntegrationTest {
    private val client = DefaultPollyClient()

    @Test
    fun canCallAServiceViaKotlin() {
        assert.that(client.describeVoices(DescribeVoicesRequest(LanguageCode.EN_AU)).voices, hasSizeGreaterThan(0))
    }

    private fun hasSizeGreaterThan(size: Int) = present(has(List<*>::size, greaterThan(size)))
}

