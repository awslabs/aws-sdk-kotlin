package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.crt.auth.signing.*
import aws.sdk.kotlin.crt.auth.signing.AwsSignatureType
import aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyHeaderType
import aws.sdk.kotlin.crt.auth.signing.AwsSigningConfig
import aws.sdk.kotlin.crt.http.HttpRequest
import aws.sdk.kotlin.runtime.endpoint.Endpoint
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HeadersBuilder
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.util.InternalApi

public data class PresignedRequestConfig(
    public val region: String,

    public val credentialsProvider: CredentialsProvider,

    public val service: String,

    public val signedHeaderKeys: Set<String>,

    public val method: HttpMethod,

    public val endpoint: Endpoint,

    public val path: String,

    public val duration: Long,

    public val hasBody: Boolean = false
)

public data class PresignedRequest(
    val url: String,
    val headers: Headers,
    val method: HttpMethod
)

@InternalApi
public suspend fun presignUrl(config: PresignedRequestConfig) : PresignedRequest {
    val crtCredentials = config.credentialsProvider.getCredentials().toCrt()
    val signingConfig: AwsSigningConfig = AwsSigningConfig.build {
        region = config.region
        service = config.service
        credentials = crtCredentials
        signatureType = AwsSignatureType.HTTP_REQUEST_VIA_HEADERS
        signedBodyHeader = AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256
        signedBodyValue = if (config.hasBody) "UNSIGNED-PAYLOAD" else null
        shouldSignHeader = { header -> config.signedHeaderKeys.contains(header) }
        expirationInSeconds = config.duration
    }
    val url = "${config.endpoint.protocol}://${config.endpoint.hostname}${config.path}"
    val request = HttpRequest(
        config.method.name,
        url,
        aws.sdk.kotlin.crt.http.Headers.build { append("host", config.endpoint.hostname) }
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
