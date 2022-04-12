/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object StsTestUtils {
    val epoch = Instant.fromIso8601("2020-10-16T03:56:00Z")
    val expectedCredentialsBase = Credentials(
        "AKIDTest",
        "test-secret",
        "test-token",
        epoch + Duration.minutes(15),
        "AssumeRoleProvider"
    )

    // see https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRole.html#API_AssumeRole_ResponseElements
    fun stsResponse(
        roleArn: String,
        expiration: Instant? = null
    ): HttpResponse {
        val roleId = roleArn.split("/").last()
        val expiry = expiration ?: expectedCredentialsBase.expiration!!
        val body = """
            <AssumeRoleResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
              <AssumeRoleResult>
              <SourceIdentity>Alice</SourceIdentity>
                <AssumedRoleUser>
                  <Arn>$roleArn</Arn>
                  <AssumedRoleId>ARO123EXAMPLE123:$roleId</AssumedRoleId>
                </AssumedRoleUser>
                <Credentials>
                  <AccessKeyId>AKIDTest</AccessKeyId>
                  <SecretAccessKey>test-secret</SecretAccessKey>
                  <SessionToken>test-token</SessionToken>
                  <Expiration>${expiry.format(TimestampFormat.ISO_8601)}</Expiration>
                </Credentials>
                <PackedPolicySize>6</PackedPolicySize>
              </AssumeRoleResult>
              <ResponseMetadata>
                <RequestId>c6104cbe-af31-11e0-8154-cbc7ccf896c7</RequestId>
              </ResponseMetadata>
            </AssumeRoleResponse>
        """.trimIndent()
        return HttpResponse(HttpStatusCode.OK, Headers.Empty, ByteArrayContent(body.encodeToByteArray()))
    }
}
