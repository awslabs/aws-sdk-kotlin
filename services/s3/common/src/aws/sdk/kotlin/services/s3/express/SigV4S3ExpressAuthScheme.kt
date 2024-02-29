/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.AuthOption
import aws.smithy.kotlin.runtime.auth.AuthSchemeId
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigner
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningAlgorithm
import aws.smithy.kotlin.runtime.http.auth.AuthScheme
import aws.smithy.kotlin.runtime.http.auth.AwsHttpSigner
import aws.smithy.kotlin.runtime.http.auth.HttpSigner
import aws.smithy.kotlin.runtime.http.auth.sigV4

public val AuthSchemeId.Companion.AwsSigV4S3Express: AuthSchemeId
    get() = AuthSchemeId("aws.auth#sigv4s3express")

/**
 * HTTP auth scheme for S3 Express One Zone authentication
 */
public class SigV4S3ExpressAuthScheme(
    httpSigner: HttpSigner,
) : AuthScheme {
    public constructor(awsSigner: AwsSigner, serviceName: String? = null) : this(
        AwsHttpSigner(
            AwsHttpSigner.Config().apply {
                signer = awsSigner
                service = serviceName
                algorithm = AwsSigningAlgorithm.SIGV4 // Note: There is no new signing algorithm for S3 Express
            },
        ),
    )

    override val schemeId: AuthSchemeId = AuthSchemeId.AwsSigV4S3Express
    override val signer: HttpSigner = S3ExpressHttpSigner(httpSigner)
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
    // Note: SigV4-S3Express has the same attributes as SigV4
    val sigV4AuthOption = sigV4(unsignedPayload, serviceName, signingRegion, disableDoubleUriEncode, normalizeUriPath)
    return AuthOption(AuthSchemeId.AwsSigV4S3Express, sigV4AuthOption.attributes)
}
