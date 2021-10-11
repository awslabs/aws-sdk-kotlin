/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.signing

import aws.sdk.kotlin.runtime.auth.credentials.Credentials
import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Predicate function used to determine if a specific header should be signed or not
 */
public typealias ShouldSignHeaderFn = (String) -> Boolean

/**
 * Configuration that tells the underlying AWS signer how to sign requests
 */
@OptIn(ExperimentalTime::class)
public class AwsSigningConfig private constructor(builder: Builder) {
    public companion object {
        public operator fun invoke(block: Builder.() -> Unit): AwsSigningConfig = Builder().apply(block).build()
    }
    /**
     * The region to sign against
     */
    public val region: String = requireNotNull(builder.region) { "signing config must specify a region" }

    /**
     * name of service to sign a request for
     */
    public val service: String = requireNotNull(builder.service) { "signing config must specify a service" }

    /**
     * Date to use during the signing process. By default it will be signed with the current time
     */
    public val date: Instant? = builder.date

    /**
     * What signing algorithm to use
     */
    public val algorithm: AwsSigningAlgorithm = builder.algorithm

    /**
     * What sort of signature should be computed?
     */
    public val signatureType: AwsSignatureType = builder.signatureType

    /**
     * It is assumed that the uri will be encoded once in preparation for transmission.  Certain services
     * do not decode before checking signature, requiring double-encoding the uri in the canonical
     * request in order to pass a signature check.
     */
    public val useDoubleUriEncode: Boolean = builder.useDoubleUriEncode

    /**
     * Controls whether or not the uri paths should be normalized when building the canonical request
     */
    public val normalizeUriPath: Boolean = builder.normalizeUriPath

    /**
     * Flag indicating if the "X-Amz-Security-Token" query param be omitted.
     * Normally, this parameter is added during signing if the credentials have a session token.
     * The only known case where this should be true is when signing a websocket handshake to IoT Core.
     */
    public val omitSessionToken: Boolean = builder.omitSessionToken

    /**
     * Optional string to use as the canonical request's body public value.
     * If string is empty, a public value will be calculated from the payload during signing.
     * Typically, this is the SHA-256 of the (request/chunk/event) payload, written as lowercase hex.
     * If this has been precalculated, it can be set here. Special public values used by certain services can also be set
     * (e.g. "UNSIGNED-PAYLOAD" "STREAMING-AWS4-HMAC-SHA256-PAYLOAD" "STREAMING-AWS4-HMAC-SHA256-EVENTS").
     */
    public val signedBodyValue: String? = builder.signedBodyValue

    /**
     * Controls what body "hash" header, if any, should be added to the canonical request and the signed request.
     * Most AWS services do not require an additional header.
     */
    public val signedBodyHeaderType: AwsSignedBodyHeaderType = builder.signedBodyHeader

    /**
     * Predicate function used to determine if a specific header should be signed or not
     */
    public val shouldSignHeader: ShouldSignHeaderFn? = builder.shouldSignHeader

    /*
     * Signing key control:
     *
     *   (1) If "credentials" is valid, use it
     *   (2) Else if "credentials_provider" is valid, query credentials from the provider and use the result
     *   (3) Else fail
     *
     */

    /**
     * AWS Credentials to sign with
     */
    public val credentials: Credentials? = builder.credentials

    /**
     * AWS credentials provider to fetch credentials from
     */
    public val credentialsProvider: CredentialsProvider? = builder.credentialsProvider

    init {
        if (credentials == null && credentialsProvider == null) {
            throw IllegalArgumentException("signing config must specify one of `credentials` or `credentialsProvider`")
        }
    }

    /**
     * If non-zero and the signing transform is query param, then signing will add X-Amz-Expires to the query
     * string, equal to the value specified here.  If this value is zero or if header signing is being used then
     * this parameter has no effect.
     */
    public val expiresAfter: Duration? = builder.expiresAfter

    public class Builder {
        public var region: String? = null
        public var service: String? = null
        public var date: Instant? = null
        public var algorithm: AwsSigningAlgorithm = AwsSigningAlgorithm.SIGV4
        public var shouldSignHeader: ShouldSignHeaderFn? = null
        public var signatureType: AwsSignatureType = AwsSignatureType.HTTP_REQUEST_VIA_HEADERS
        public var useDoubleUriEncode: Boolean = true
        public var normalizeUriPath: Boolean = true
        public var omitSessionToken: Boolean = false
        public var signedBodyValue: String? = null
        public var signedBodyHeader: AwsSignedBodyHeaderType = AwsSignedBodyHeaderType.NONE
        public var credentials: Credentials? = null
        public var credentialsProvider: CredentialsProvider? = null
        public var expiresAfter: Duration? = null

        internal fun build(): AwsSigningConfig = AwsSigningConfig(this)
    }
}

internal suspend fun AwsSigningConfig.getCredentials(): Credentials =
    credentials ?: checkNotNull(credentialsProvider).getCredentials()

/**
 * Defines the AWS signature version to use
 */
public enum class AwsSigningAlgorithm {
    /**
     * AWS Signature Version 4
     * see: https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html
     */
    SIGV4,

    /**
     * AWS Signature Version 4 Asymmetric
     */
    SIGV4_ASYMMETRIC;
}

/**
 * Defines the type of signature to compute
 */
public enum class AwsSignatureType {
    /**
     * A signature for a full http request should be computed and applied via headers
     */
    HTTP_REQUEST_VIA_HEADERS,

    /**
     * A signature for a full http request should be computed and applied via query parameters
     */
    HTTP_REQUEST_VIA_QUERY_PARAMS,

    /**
     * Compute a signature for a payload chunk
     */
    HTTP_REQUEST_CHUNK,

    /**
     * Compute a signature for an event stream
     */
    HTTP_REQUEST_EVENT;
}

/**
 * Controls if signing adds a header containing the canonical request's (hashed) body value
 */
public enum class AwsSignedBodyHeaderType {
    /**
     * Do not add a header
     */
    NONE,

    /**
     * Add the "x-amz-content-sha256" header with the canonical request's body value
     */
    X_AMZ_CONTENT_SHA256;
}

/**
 * A set of string constants for various canonical request payload values. If signedBodyValue is not null
 * then the value will also be reflected in X-Amz-Content-Sha256
 */
public object AwsSignedBodyValue {
    public const val EMPTY_SHA256: String = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    public const val UNSIGNED_PAYLOAD: String = "UNSIGNED-PAYLOAD"
    public const val STREAMING_AWS4_HMAC_SHA256_PAYLOAD: String = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD"
    public const val STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD: String = "STREAMING-AWS4-ECDSA-P256-SHA256-PAYLOAD"
    public const val STREAMING_AWS4_HMAC_SHA256_EVENTS: String = "STREAMING-AWS4-HMAC-SHA256-EVENTS"
}

internal fun AwsSignedBodyHeaderType.toCrt(): aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyHeaderType = when (this) {
    AwsSignedBodyHeaderType.NONE -> aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyHeaderType.NONE
    AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256 -> aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256
}

internal fun AwsSignatureType.toCrt(): aws.sdk.kotlin.crt.auth.signing.AwsSignatureType = when (this) {
    AwsSignatureType.HTTP_REQUEST_VIA_HEADERS -> aws.sdk.kotlin.crt.auth.signing.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS
    AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS -> aws.sdk.kotlin.crt.auth.signing.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS
    AwsSignatureType.HTTP_REQUEST_CHUNK -> aws.sdk.kotlin.crt.auth.signing.AwsSignatureType.HTTP_REQUEST_CHUNK
    AwsSignatureType.HTTP_REQUEST_EVENT -> aws.sdk.kotlin.crt.auth.signing.AwsSignatureType.HTTP_REQUEST_EVENT
}

internal fun AwsSigningAlgorithm.toCrt(): aws.sdk.kotlin.crt.auth.signing.AwsSigningAlgorithm = when (this) {
    AwsSigningAlgorithm.SIGV4 -> aws.sdk.kotlin.crt.auth.signing.AwsSigningAlgorithm.SIGV4
    AwsSigningAlgorithm.SIGV4_ASYMMETRIC -> aws.sdk.kotlin.crt.auth.signing.AwsSigningAlgorithm.SIGV4_ASYMMETRIC
}

internal fun Credentials.toCrt(): aws.sdk.kotlin.crt.auth.credentials.Credentials =
    aws.sdk.kotlin.crt.auth.credentials.Credentials(accessKeyId, secretAccessKey, sessionToken)

@OptIn(ExperimentalTime::class)
internal suspend fun AwsSigningConfig.toCrt(): aws.sdk.kotlin.crt.auth.signing.AwsSigningConfig {
    val config = this
    // NOTE: we cannot pass a credentialsProvider due to https://github.com/awslabs/aws-crt-kotlin/issues/15
    // the underlying implementation will hang/fail to sign
    val resolvedCredentials = config.credentials ?: config.credentialsProvider?.getCredentials()

    return aws.sdk.kotlin.crt.auth.signing.AwsSigningConfig.build {
        algorithm = config.algorithm.toCrt()
        credentials = resolvedCredentials?.toCrt()

        date = config.date?.epochMilliseconds

        config.expiresAfter?.let {
            expirationInSeconds = it.inWholeSeconds
        }

        normalizeUriPath = config.normalizeUriPath
        omitSessionToken = config.omitSessionToken

        region = config.region
        service = config.service

        shouldSignHeader = config.shouldSignHeader
        signatureType = config.signatureType.toCrt()
        signedBodyHeader = config.signedBodyHeaderType.toCrt()
        signedBodyValue = config.signedBodyValue
        useDoubleUriEncode = config.useDoubleUriEncode
    }
}
