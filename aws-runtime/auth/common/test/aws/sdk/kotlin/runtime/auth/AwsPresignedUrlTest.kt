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
import kotlin.test.Test

class AwsPresignedUrlTest {

    @Test
    fun testPolly() = runSuspendTest {
        // polly.us-east-1.amazonaws.com/v1/speech?Text=hello%20world%21&VoiceId=Salli&OutputFormat=pcm&
        // X-Amz-Algorithm=AWS4-HMAC-SHA256&
        // X-Amz-Date=20210622T215643Z&
        // X-Amz-SignedHeaders=host&
        // X-Amz-Expires=2700&
        // X-Amz-Credential=AKIAV5L256TMEABNYFE5%2F20210622%2Fus-east-1%2Fpolly%2Faws4_request&
        // X-Amz-Signature=4f3e24d27e48a49f97fe5f28422bb32a4d69e871514c00165733b9601760e6fc

        // polly.us-east-1.amazonaws.com/v1/speech?Text=hello%20world%21&VoiceId=Salli&OutputFormat=pcm&
        // X-Amz-Algorithm=AWS4-HMAC-SHA256&
        // X-Amz-Date=20210622T230637Z&
        // X-Amz-SignedHeaders=host&
        // X-Amz-Expires=2700&
        // X-Amz-Credential=a%2F20210622%2Fus-west-2%2Fpolly%2Faws4_request&
        // X-Amz-Signature=6c50a4504846d51dc4e8658ea93ce17e56b34f8dd0661792f2ab0125f04c71d4

        val signingConfig: AwsSigningConfig = AwsSigningConfig.build {
            region = "us-west-2"
            service = "polly"
            credentials = Credentials("a", "b", null)
            algorithm = AwsSigningAlgorithm.SIGV4
            signatureType = AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS
            omitSessionToken = true
            normalizeUriPath = true
            useDoubleUriEncode = true
            signedBodyHeader = AwsSignedBodyHeaderType.NONE
            shouldSignHeader = { header -> header == "host" }
            expirationInSeconds = 2700
        }

        val request = HttpRequest(
            "GET",
            "polly.us-east-1.amazonaws.com/v1/speech?Text=hello%20world%21&VoiceId=Salli&OutputFormat=pcm",
            Headers.build { append("host", "polly.us-east-1.amazonaws.com") }
        )
        val signedRequest = AwsSigner.signRequest(request, signingConfig)

        println(signedRequest.encodedPath.toString())
    }
}