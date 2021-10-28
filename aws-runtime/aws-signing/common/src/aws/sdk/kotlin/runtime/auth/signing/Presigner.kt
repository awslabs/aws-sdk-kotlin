/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.signing

import aws.sdk.kotlin.crt.auth.signing.AwsSignatureType
import aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyHeaderType
import aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyValue
import aws.sdk.kotlin.crt.auth.signing.AwsSigner
import aws.sdk.kotlin.crt.auth.signing.AwsSigningConfig
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider
import aws.sdk.kotlin.runtime.crt.path
import aws.sdk.kotlin.runtime.crt.queryParameters
import aws.sdk.kotlin.runtime.crt.toCrtHeaders
import aws.sdk.kotlin.runtime.crt.toSdkHeaders
import aws.sdk.kotlin.runtime.endpoint.AwsEndpointResolver
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.Protocol
import aws.smithy.kotlin.runtime.http.QueryParameters
import aws.smithy.kotlin.runtime.http.Url
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.sdk.kotlin.crt.http.HttpRequest as CrtHttpRequest

/**
 * The service configuration details for a presigned request
 *
 * @property region The AWS region to which the request is going.
 * @property signingName The signing name used to sign the request.
 * @property serviceId the service id used to sign the request.
 * @property endpointResolver Resolves the endpoint to determine where the request should be sent.
 * @property credentialsProvider Resolves credentials to sign the request with.
 */
public interface ServicePresignConfig {
    public val region: String
    public val signingName: String
    public val serviceId: String
    public val endpointResolver: AwsEndpointResolver
    public val credentialsProvider: CredentialsProvider
}

/**
 * Where the signature is placed in the presigned request
 * @property HEADER Signing details to be placed in a header.
 * @property QUERY_STRING Signing details to be added to the query string.
 */
public enum class SigningLocation {
    HEADER,
    QUERY_STRING,
}

/**
 * Configuration of a presigned request
 * @property method HTTP method of the presigned request
 * @property path HTTP path of the presigned request
 * @property queryString the HTTP querystring of the presigned request
 * @property durationSeconds Number of seconds that the request will be valid for after being signed.
 * @property hasBody Specifies if the request contains a body
 * @property signingLocation Specifies where the signing information should be placed in the presigned request
 * @property additionalHeaders Custom headers that should be signed as part of the request
 */
public data class PresignedRequestConfig(
    public val method: HttpMethod,
    public val path: String,
    public val queryString: QueryParameters = QueryParameters.Empty,
    public val durationSeconds: Long,
    public val hasBody: Boolean = false,
    public val signingLocation: SigningLocation,
    public val additionalHeaders: Headers = Headers.Empty
)

/**
 * Generate a presigned request given the service and operation configurations.
 * @param serviceConfig The service configuration to use in signing the request
 * @param requestConfig The presign configuration to use in signing the request
 * @return a [HttpRequest] that can be executed by any HTTP client within the specified duration.
 */
@InternalSdkApi
public suspend fun createPresignedRequest(serviceConfig: ServicePresignConfig, requestConfig: PresignedRequestConfig): HttpRequest {
    val crtCredentials = serviceConfig.credentialsProvider.getCredentials().toCrt()
    val endpoint = serviceConfig.endpointResolver.resolve(serviceConfig.serviceId, serviceConfig.region)

    val signingConfig: AwsSigningConfig = AwsSigningConfig.build {
        region = endpoint.credentialScope?.region ?: serviceConfig.region
        service = endpoint.credentialScope?.service ?: serviceConfig.signingName
        credentials = crtCredentials
        signatureType = if (requestConfig.signingLocation == SigningLocation.HEADER) AwsSignatureType.HTTP_REQUEST_VIA_HEADERS else AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS
        signedBodyHeader = AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256
        signedBodyValue = if (requestConfig.hasBody) AwsSignedBodyValue.UNSIGNED_PAYLOAD else null
        expirationInSeconds = requestConfig.durationSeconds
    }

    val unsignedUrl = Url(
        scheme = Protocol.HTTPS,
        host = endpoint.endpoint.uri.host,
        port = endpoint.endpoint.uri.port,
        path = requestConfig.path,
        parameters = requestConfig.queryString,
    )

    val request = CrtHttpRequest(
        requestConfig.method.name,
        unsignedUrl.encodedPath,
        aws.sdk.kotlin.crt.http.Headers.build {
            append("Host", endpoint.endpoint.uri.host)
            appendAll(requestConfig.additionalHeaders.toCrtHeaders())
        }
    )
    val signedRequest = AwsSigner.signRequest(request, signingConfig)

    return HttpRequest(
        method = HttpMethod.parse(signedRequest.method),
        url = Url(
            scheme = Protocol.HTTPS,
            host = endpoint.endpoint.uri.host,
            port = endpoint.endpoint.uri.port,
            path = signedRequest.path(),
            parameters = signedRequest.queryParameters() ?: QueryParameters.Empty,
            encodeParameters = false,
        ),
        headers = signedRequest.headers.toSdkHeaders(),
        body = HttpBody.Empty
    )
}
