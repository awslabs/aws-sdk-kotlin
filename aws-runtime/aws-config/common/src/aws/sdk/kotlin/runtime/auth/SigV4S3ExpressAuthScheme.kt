/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.AuthOption
import aws.smithy.kotlin.runtime.auth.AuthSchemeId
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigner
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningAlgorithm
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningAttributes
import aws.smithy.kotlin.runtime.auth.awssigning.HashSpecification
import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.collections.MutableAttributes
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import aws.smithy.kotlin.runtime.collections.mutableAttributes
import aws.smithy.kotlin.runtime.http.auth.AuthScheme
import aws.smithy.kotlin.runtime.http.auth.AwsHttpSigner

/**
 * HTTP auth scheme for S3 Express One Zone authentication
 */
public class SigV4S3ExpressAuthScheme(
    config: AwsHttpSigner.Config,
) : AuthScheme {
    public constructor(awsSigner: AwsSigner, serviceName: String? = null) : this(
        AwsHttpSigner.Config().apply {
            signer = awsSigner
            service = serviceName
            algorithm = AwsSigningAlgorithm.SIGV4_S3EXPRESS
        },
    )

    override val schemeId: AuthSchemeId = AuthSchemeId.AwsSigV4S3Express
    override val signer: AwsHttpSigner = AwsHttpSigner(config)
}

/**
 * Create a new [AuthOption] for the [SigV4S3ExpressAuthScheme]
 * @param unsignedPayload set the signing attribute to indicate the signer should use unsigned payload.
 * @param serviceName override the service name to sign for
 * @param signingRegion override the signing region to sign for
 * @param disableDoubleUriEncode disable double URI encoding
 * @param normalizeUriPath flag indicating if the URI path should be normalized when forming the canonical request
 * @return auth scheme option representing the [SigV4S3ExpressAuthScheme]
 */
@InternalApi
public fun sigV4S3Express(
    unsignedPayload: Boolean = false,
    serviceName: String? = null,
    signingRegion: String? = null,
    disableDoubleUriEncode: Boolean? = null,
    normalizeUriPath: Boolean? = null,
): AuthOption {
    val attrs = if (unsignedPayload || serviceName != null || signingRegion != null || disableDoubleUriEncode != null || normalizeUriPath != null) {
        val mutAttrs = mutableAttributes()
        mutAttrs.setNotBlank(AwsSigningAttributes.SigningRegion, signingRegion)
        setCommonSigV4Attrs(mutAttrs, unsignedPayload, serviceName, disableDoubleUriEncode, normalizeUriPath)
        mutAttrs
    } else {
        emptyAttributes()
    }
    return AuthOption(AuthSchemeId.AwsSigV4S3Express, attrs)
}

// FIXME Copied from SigV4AuthScheme.kt
internal fun MutableAttributes.setNotBlank(key: AttributeKey<String>, value: String?) {
    if (!value.isNullOrBlank()) set(key, value)
}

internal fun setCommonSigV4Attrs(
    attrs: MutableAttributes,
    unsignedPayload: Boolean = false,
    serviceName: String? = null,
    disableDoubleUriEncode: Boolean? = null,
    normalizeUriPath: Boolean? = null,
) {
    if (unsignedPayload) {
        attrs[AwsSigningAttributes.HashSpecification] = HashSpecification.UnsignedPayload
    }
    attrs.setNotBlank(AwsSigningAttributes.SigningService, serviceName)
    if (disableDoubleUriEncode != null) {
        attrs[AwsSigningAttributes.UseDoubleUriEncode] = !disableDoubleUriEncode
    }

    if (normalizeUriPath != null) {
        attrs[AwsSigningAttributes.NormalizeUriPath] = normalizeUriPath
    }
}