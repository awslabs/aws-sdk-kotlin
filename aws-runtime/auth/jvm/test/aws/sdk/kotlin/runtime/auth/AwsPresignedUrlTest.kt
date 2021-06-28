package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.crt.auth.credentials.Credentials
import aws.sdk.kotlin.crt.auth.signing.*
import aws.sdk.kotlin.crt.auth.signing.AwsSignatureType
import aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyHeaderType
import aws.sdk.kotlin.crt.auth.signing.AwsSigningAlgorithm
import aws.sdk.kotlin.crt.auth.signing.AwsSigningConfig
import aws.sdk.kotlin.crt.http.Headers
import aws.sdk.kotlin.crt.http.HttpRequest
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.test.Test

class AwsPresignedUrlTest2 {

    @Test
    fun testPollyPresignSynthesizeSpeech() = runSuspendTest {
        val creds = DefaultChainCredentialsProvider().crtProvider.getCredentials()

        val signingConfig: AwsSigningConfig = AwsSigningConfig.build {
            region = "us-east-1"
            service = "polly"
            credentials = creds
            signatureType = AwsSignatureType.HTTP_REQUEST_VIA_HEADERS
            signedBodyHeader = AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256
        }

        val request = HttpRequest(
            "GET",
            "polly.us-east-1.amazonaws.com/v1/speech?Text=hello%20world%21&VoiceId=Salli&OutputFormat=pcm",
            Headers.build { append("host", "polly.us-east-1.amazonaws.com") }
        )
        val signedRequest = AwsSigner.signRequest(request, signingConfig)
        val url = URL("https://${signedRequest.encodedPath}")

        println(signedRequest.encodedPath.toString())

        var urlConnection: HttpsURLConnection? = null
        try {
            urlConnection = url.openConnection() as HttpsURLConnection? ?: error("failed to open connection")
            signedRequest.headers.forEach { key, values -> urlConnection.setRequestProperty(key, values.first()) }
            urlConnection.connect()
            println(urlConnection.responseCode)
            println(urlConnection.getHeaderField("Content-Type"))
        } finally {
            urlConnection!!.inputStream.close()
        }
    }

    @Test
    fun testS3PresignGetObject() = runSuspendTest {
        val creds = DefaultChainCredentialsProvider().crtProvider.getCredentials()

        val signingConfig: AwsSigningConfig = AwsSigningConfig.build {
            region = "us-east-2"
            service = "s3"
            credentials = creds
            signatureType = AwsSignatureType.HTTP_REQUEST_VIA_HEADERS
            signedBodyHeader = AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256
        }

        val request = HttpRequest(
            "GET",
            "kgwh-test-bucket.s3.us-east-2.amazonaws.com/boo.txt",
            Headers.build { append("host", "kgwh-test-bucket.s3.us-east-2.amazonaws.com") }
        )

        val signedRequest = AwsSigner.signRequest(request, signingConfig)
        val x = signedRequest.headers
        val url = URL("https://${signedRequest.encodedPath}")

        println(signedRequest.encodedPath.toString())

        var urlConnection: HttpsURLConnection? = null
        try {
            urlConnection = url.openConnection() as HttpsURLConnection? ?: error("failed to open connection")
            urlConnection.setRequestProperty("x-amz-content-sha256", signedRequest.headers["x-amz-content-sha256"])
            urlConnection.setRequestProperty("X-Amz-Date", signedRequest.headers["X-Amz-Date"])
            urlConnection.setRequestProperty("Authorization", signedRequest.headers["Authorization"])

            urlConnection.connect()
            println(urlConnection.responseCode)
            println(urlConnection.getHeaderField("Content-Type"))
        } finally {
            urlConnection!!.inputStream.close()
        }
    }
}