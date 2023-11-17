package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObject
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.util.Attributes
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
class Handle200ErrorsInterceptorTest {

    object TestCredentialsProvider: CredentialsProvider {
        override suspend fun resolve(attributes: Attributes): Credentials = Credentials("AKID", "SECRET")
    }

    @Test
    fun testHandle200Errors() = runTest {
        val content = """
            <Error>
                <Code>SlowDown</Code>
                <Message>Please reduce your request rate.</Message>
                <RequestId>K2H6N7ZGQT6WHCEG</RequestId>
                <HostId>WWoZlnK4pTjKCYn6eNV7GgOurabfqLkjbSyqTvDMGBaI9uwzyNhSaDhOCPs8paFGye7S6b/AB3A=</HostId>
            </Error>
        """.trimIndent().encodeToByteArray()

        val s3 = S3Client {
            credentialsProvider = TestCredentialsProvider
            httpClient = buildTestConnection {
                expect(HttpResponse(HttpStatusCode.OK, body = HttpBody.fromBytes(content)))
            }
        }

        s3.deleteObject { bucket = "test" }

    }
}