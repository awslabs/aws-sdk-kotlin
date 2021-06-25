package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.crt.auth.signing.*
import aws.sdk.kotlin.crt.auth.signing.AwsSignatureType
import aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyHeaderType
import aws.sdk.kotlin.crt.auth.signing.AwsSigningAlgorithm
import aws.sdk.kotlin.crt.auth.signing.AwsSigningConfig
import aws.sdk.kotlin.crt.http.HttpRequest
import aws.sdk.kotlin.runtime.endpoint.Endpoint
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HeadersBuilder
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.util.InternalApi

public data class PresignedRequestConfig(
    /**
     * AWS Region the client was configured with. Note that this is not always the signing region in the case of global
     * services like IAM.
     */
    public val region: String,

    /**
     * Specifies zero or more credential providers that will be called to resolve credentials before making
     * AWS service calls.  If not provided a default credential provider chain is used.
     */
    public val credentialsProvider: CredentialsProvider,

    public val service: String,

    public val signedHeaderKeys: Set<String> = setOf("host"),

    public val method: HttpMethod,

    public val endpoint: Endpoint,

    public val path: String
)

public data class PresignedRequest(
    val url: String,
    val headers: Headers,
    val method: HttpMethod
)

@InternalApi
public suspend fun presignUrl(presignedClientConfig: PresignedRequestConfig, url: String) : PresignedRequest {
    val crtCredentials = presignedClientConfig.credentialsProvider.getCredentials().toCrt()
    val signingConfig: AwsSigningConfig = AwsSigningConfig.build {
        region = presignedClientConfig.region
        service = presignedClientConfig.service.toLowerCase()
        credentials = crtCredentials
        algorithm = AwsSigningAlgorithm.SIGV4
        signatureType = AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS
        omitSessionToken = true
        normalizeUriPath = true
        useDoubleUriEncode = true
        signedBodyHeader = AwsSignedBodyHeaderType.NONE
        shouldSignHeader = { header -> presignedClientConfig.signedHeaderKeys.contains(header) }
        expirationInSeconds = 2700
    }

    val request = HttpRequest(
        presignedClientConfig.method.name,
        url,
        aws.sdk.kotlin.crt.http.Headers.build { append("host", presignedClientConfig.endpoint.hostname) }
    )
    val signedRequest = AwsSigner.signRequest(request, signingConfig)

    return PresignedRequest(signedRequest.encodedPath, signedRequest.headers.toSdkHeaders(), HttpMethod.parse(signedRequest.method))
}

private fun aws.sdk.kotlin.crt.http.Headers.toSdkHeaders(): Headers {
    val hdrs =  HeadersBuilder()

    forEach { key, values ->
        hdrs.appendAll(key, values)
    }

    return hdrs.build()
}
