package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.crt.auth.signing.AwsSignatureType
import aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyHeaderType
import aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyValue
import aws.sdk.kotlin.crt.auth.signing.AwsSigner
import aws.sdk.kotlin.crt.auth.signing.AwsSigningConfig
import aws.sdk.kotlin.crt.http.HttpRequest
import aws.sdk.kotlin.crt.toCrtHeaders
import aws.sdk.kotlin.crt.toSdkHeaders
import aws.sdk.kotlin.runtime.endpoint.EndpointResolver
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.util.InternalApi

/**
 * The service configuration details for a presigned request
 */
public interface ServicePresignConfig {
    /**
     * The AWS region to which the request is going.
     */
    public val region: String

    /**
     * The service name used to sign the request.
     */
    public val serviceName: String

    /**
     * Resolves the endpoint to determine where the request should be sent.
     */
    public val endpointResolver: EndpointResolver

    /**
     * Resolves credentials to sign the request with.
     */
    public val credentialsProvider: CredentialsProvider
}

/**
 * Where the signature is placed in the presigned request
 */
public enum class SigningLocation {
    /**
     * Signing details to be placed in a header.
     */
    HEADER,

    /**
     * Signing details to be added to the query string.
     */
    QUERY_STRING
}

/**
 * Configuration of a presigned request
 */
public data class PresignedRequestConfig(
    /**
     * HTTP method of the presigned request
     */
    public val method: HttpMethod,
    /**
     * HTTP path of the presigned request
     */
    public val path: String,
    /**
     * Number of seconds that the request will be valid for after
     * being signed.
     */
    public val durationSeconds: Long,
    /**
     * Specifies if the request contains a body
     */
    public val hasBody: Boolean = false,
    /**
     * Specifies where the signing information should be placed in the presigned request
     */
    public val signingLocation: SigningLocation,
    /**
     * Custom headers that should be signed as part of the request
     */
    public val additionalHeaders: Headers = Headers.Empty
)

/**
 * Properties of an HTTP request that has been presigned
 */
public data class PresignedRequest(
    /**
     * HTTP url of the presigned request
     */
    val url: String,
    /**
     * Headers that must be sent with the request
     */
    val headers: Headers,
    /**
     * HTTP method to use when initiating the request
     */
    val method: HttpMethod
)

/**
 * Generate a presigned request given the service and operation configurations.
 * @param serviceConfig The service configuration to use in signing the request
 * @param requestConfig The presign configuration to use in signing the request
 * @return a [PresignedRequest] that can be executed by any HTTP client within the specified duration.
 */
@InternalApi
public suspend fun createPresignedRequest(serviceConfig: ServicePresignConfig, requestConfig: PresignedRequestConfig): PresignedRequest {
    val crtCredentials = serviceConfig.credentialsProvider.getCredentials().toCrt()
    val endpoint = serviceConfig.endpointResolver.resolve(serviceConfig.serviceName, serviceConfig.region)

    val signingConfig: AwsSigningConfig = AwsSigningConfig.build {
        region = serviceConfig.region
        service = endpoint.signingName ?: serviceConfig.serviceName
        credentials = crtCredentials
        signatureType = if (requestConfig.signingLocation == SigningLocation.HEADER) AwsSignatureType.HTTP_REQUEST_VIA_HEADERS else AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS
        signedBodyHeader = AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256 // if (requestConfig.hasBody) AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256 else AwsSignedBodyHeaderType.NONE
        signedBodyValue = if (requestConfig.hasBody) AwsSignedBodyValue.UNSIGNED_PAYLOAD else null
        expirationInSeconds = requestConfig.durationSeconds
    }

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
