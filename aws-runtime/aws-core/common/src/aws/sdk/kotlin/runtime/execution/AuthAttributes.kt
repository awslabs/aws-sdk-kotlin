/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.execution

import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider
import aws.smithy.kotlin.runtime.client.ClientOption
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.AttributeKey
import aws.smithy.kotlin.runtime.util.InternalApi

/**
 * Operation (execution) options related to authorization
 */
public object AuthAttributes {
    /**
     * AWS region to be used for signing the request
     */
    public val SigningRegion: ClientOption<String> = ClientOption("AwsSigningRegion")

    /**
     * The signature version 4 service signing name to use in the credential scope when signing requests.
     * See: https://docs.aws.amazon.com/general/latest/gr/sigv4_elements.html
     */
    public val SigningService: ClientOption<String> = ClientOption("AwsSigningService")

    /**
     * Mark a request payload as unsigned
     * See: https://awslabs.github.io/smithy/1.0/spec/aws/aws-auth.html#aws-auth-unsignedpayload-trait
     */
    public val UnsignedPayload: ClientOption<Boolean> = ClientOption("UnsignedPayload")

    /**
     * Override the date to complete the signing process with. Defaults to current time when not specified.
     *
     * NOTE: This is an advanced configuration option that does not normally need to be set manually.
     */
    public val SigningDate: ClientOption<Instant> = ClientOption("SigningDate")

    /**
     * The [CredentialsProvider] to complete the signing process with. Defaults to the provider configured
     * on the service client.
     *
     * NOTE: This is an advanced configuration option that does not normally need to be set manually.
     */
    public val CredentialsProvider: ClientOption<CredentialsProvider> = ClientOption("CredentialsProvider")

    /**
     * The precomputed signed body value. See [aws.sdk.kotlin.runtime.auth.signing.AwsSigningConfig.signedBodyValue].
     *
     * NOTE: This is an advanced configuration option that does not normally need to be set manually.
     */
    public val SignedBodyValue: ClientOption<String> = ClientOption("SignedBodyValue")

    /**
     * The signed body header type. See [aws.sdk.kotlin.runtime.auth.signing.AwsSigningConfig.signedBodyHeaderType].
     *
     * NOTE: This is an advanced configuration option that does not normally need to be set manually.
     */
    public val SignedBodyHeaderType: ClientOption<String> = ClientOption("SignedBodyHeaderType")

    /**
     * The signature of the HTTP request. This will only exist after the request has been signed!
     */
    @InternalApi
    public val RequestSignature: AttributeKey<ByteArray> = AttributeKey("AWS_HTTP_SIGNATURE")
}
