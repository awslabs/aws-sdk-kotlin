/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.imds

import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.Url
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.url
import aws.smithy.kotlin.runtime.http.response.HttpResponse

fun tokenRequest(host: String, ttl: Int): HttpRequest = HttpRequest {
    val parsed = Url.parse(host)
    url(parsed)
    method = HttpMethod.PUT
    url.path = "/latest/api/token"
    headers.append(X_AWS_EC2_METADATA_TOKEN_TTL_SECONDS, ttl.toString())
}

fun tokenResponse(ttl: Int, token: String): HttpResponse = HttpResponse(
    HttpStatusCode.OK,
    Headers {
        append(X_AWS_EC2_METADATA_TOKEN_TTL_SECONDS, ttl.toString())
    },
    ByteArrayContent(token.encodeToByteArray())
)

fun imdsRequest(url: String, token: String): HttpRequest = HttpRequest {
    val parsed = Url.parse(url)
    url(parsed)
    headers.append(X_AWS_EC2_METADATA_TOKEN, token)
}

fun imdsResponse(body: String): HttpResponse = HttpResponse(
    HttpStatusCode.OK,
    Headers.Empty,
    ByteArrayContent(body.encodeToByteArray())
)
