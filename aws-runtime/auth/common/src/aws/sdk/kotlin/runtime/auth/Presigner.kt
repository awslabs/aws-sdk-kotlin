package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.crt.auth.signing.AwsSignatureType
import aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyHeaderType
import aws.sdk.kotlin.crt.auth.signing.AwsSigner
import aws.sdk.kotlin.crt.auth.signing.AwsSigningConfig
import aws.sdk.kotlin.crt.http.HttpRequest
import aws.sdk.kotlin.runtime.endpoint.EndpointResolver
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HeadersBuilder
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.util.InternalApi

interface ServicePresignConfig {
    public val region: String
    public val serviceName: String
    public val endpointResolver: EndpointResolver
    public val credentialsProvider: CredentialsProvider?
}

public enum class SigningLocation {
    HEADER, QUERY_STRING
}

public data class PresignedRequestConfig(
    public val signedHeaderKeys: Set<String>,
    public val method: HttpMethod,
    public val path: String,
    public val duration: Long,
    public val hasBody: Boolean = false,
    public val signingLocation: SigningLocation
)

public data class PresignedRequest(
    val url: String,
    val headers: Headers,
    val method: HttpMethod
)

@InternalApi
public suspend fun presignUrl(serviceConfig: ServicePresignConfig, requestConfig: PresignedRequestConfig) : PresignedRequest {
    val crtCredentials = serviceConfig.credentialsProvider!!.getCredentials().toCrt()
    val signingConfig: AwsSigningConfig = AwsSigningConfig.build {
        region = serviceConfig.region
        service = serviceConfig.serviceName.toLowerCase()
        credentials = crtCredentials
        signatureType = if (requestConfig.signingLocation == SigningLocation.HEADER) AwsSignatureType.HTTP_REQUEST_VIA_HEADERS else AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS
        signedBodyHeader = AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256
        signedBodyValue = if (requestConfig.hasBody) "UNSIGNED-PAYLOAD" else null
        shouldSignHeader = { header -> requestConfig.signedHeaderKeys.contains(header) }
        expirationInSeconds = requestConfig.duration
    }
    val endpoint = serviceConfig.endpointResolver.resolve(serviceConfig.serviceName, serviceConfig.region!!)
    val url = "${endpoint.protocol}://${endpoint.hostname}${requestConfig.path}"
    val request = HttpRequest(
        requestConfig.method.name,
        url,
        aws.sdk.kotlin.crt.http.Headers.build { append("host", endpoint.hostname) }
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
