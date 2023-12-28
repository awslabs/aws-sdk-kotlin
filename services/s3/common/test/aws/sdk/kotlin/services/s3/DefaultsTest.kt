/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3

import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.smithy.kotlin.runtime.auth.AuthSchemeId
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSignedBodyHeader
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningAttributes
import aws.smithy.kotlin.runtime.client.RequestInterceptorContext
import aws.smithy.kotlin.runtime.client.SdkClientOption
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.http.auth.*
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.identity.IdentityProvider
import aws.smithy.kotlin.runtime.identity.IdentityProviderConfig
import aws.smithy.kotlin.runtime.io.use
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DefaultsTest {

    /**
     * Test execution context gets filled with defaults. This applies to any generated client but S3 has some additional
     * things added.
     *
     * See [aws-sdk-kotlin#1164](https://github.com/awslabs/aws-sdk-kotlin/issues/1165S)
     */
    @Test
    fun testDefaultExecutionContext() = runTest {
        val mockEngine = buildTestConnection { expect() }
        val noAuth = object : AuthScheme {
            override val schemeId: AuthSchemeId = AuthSchemeId.AwsSigV4
            override val signer: HttpSigner = AnonymousHttpSigner
            override fun identityProvider(identityProviderConfig: IdentityProviderConfig): IdentityProvider = AnonymousIdentityProvider
        }

        S3Client {
            region = "us-east-1"
            httpClient = mockEngine
            authSchemes = listOf(noAuth)
            interceptors += object : HttpInterceptor {
                override fun readBeforeExecution(context: RequestInterceptorContext<Any>) {
                    assertNotNull(context.executionContext[SdkClientOption.ClientName])
                    assertNotNull(context.executionContext[SdkClientOption.LogMode])
                    assertNotNull(context.executionContext[AwsSigningAttributes.CredentialsProvider])

                    assertEquals(region, context.executionContext[AwsClientOption.Region])
                    assertEquals(region, context.executionContext[AwsSigningAttributes.SigningRegion])
                    assertEquals("s3", context.executionContext[AwsSigningAttributes.SigningService])

                    // S3 specific
                    assertEquals(false, context.executionContext[AwsSigningAttributes.NormalizeUriPath])
                    assertEquals(false, context.executionContext[AwsSigningAttributes.UseDoubleUriEncode])
                    assertEquals(AwsSignedBodyHeader.X_AMZ_CONTENT_SHA256, context.executionContext[AwsSigningAttributes.SignedBodyHeader])
                }
            }
        }.use { s3 ->
            s3.listBuckets()
        }
    }
}
