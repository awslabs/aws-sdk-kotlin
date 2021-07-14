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

/**
 * The service configuration details for a presigned request
 */
interface ServicePresignConfig {
    public val region: String
    public val serviceName: String
    public val endpointResolver: EndpointResolver
    public val credentialsProvider: CredentialsProvider
}

/**
 * Where the signature is placed in the presigned request
 */
public enum class SigningLocation {
    HEADER, QUERY_STRING
}

/**
 * Configuration of a presigned request
 */
public data class PresignedRequestConfig(
    public val signedHeaderKeys: Set<String>,
    public val method: HttpMethod,
    public val path: String,
    public val duration: Long,
    public val hasBody: Boolean = false,
    public val signingLocation: SigningLocation,
    public val additionalHeaders: Headers = Headers.Empty
)

/**
 * Properties of an HTTP request that has been presigned
 */
public data class PresignedRequest(
    val url: String,
    val headers: Headers,
    val method: HttpMethod
)

/**
 * Generate a presigned request given the service and operation configurations.
 * @param serviceConfig The service configuration to use in signing the request
 * @param requestConfig The presign configuration to use in signing the request
 * @return a [PresignedRequest] that can be executed by any HTTP client within the specified duration.
 */
@InternalApi
public suspend fun createPresignedRequest(serviceConfig: ServicePresignConfig, requestConfig: PresignedRequestConfig) : PresignedRequest {
    val crtCredentials = serviceConfig.credentialsProvider?.getCredentials()?.toCrt() ?: error("Must specify credentialsProvider.")
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
    val headers = aws.sdk.kotlin.crt.http.Headers.build {
        append("Host", endpoint.hostname)
        appendAll(requestConfig.additionalHeaders.toCrtHeaders())
    }
    val request = HttpRequest(
        requestConfig.method.name,
        url,
        headers
    )
    val signedRequest = AwsSigner.signRequest(request, signingConfig)

    return PresignedRequest(signedRequest.encodedPath, signedRequest.headers.toSdkHeaders(), HttpMethod.parse(signedRequest.method))
}

// Convert CRT header type to SDK header type
private fun aws.sdk.kotlin.crt.http.Headers.toSdkHeaders(): Headers {
    val hdrs =  HeadersBuilder()

    forEach { key, values ->
        hdrs.appendAll(key, values)
    }

    return hdrs.build()
}

// Convert SDK header type to CRT header type
private fun Headers.toCrtHeaders(): aws.sdk.kotlin.crt.http.Headers {
    val hdrs =  aws.sdk.kotlin.crt.http.HeadersBuilder()

    forEach { key, values ->
        hdrs.appendAll(key, values)
    }

    return hdrs.build()
}
