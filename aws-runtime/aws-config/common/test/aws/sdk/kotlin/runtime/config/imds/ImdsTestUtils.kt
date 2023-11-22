/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.imds

import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.url
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.net.url.Url

fun tokenRequest(host: String, ttl: Int): HttpRequest = HttpRequest {
    url(Url.parse(host))
    method = HttpMethod.PUT
    url.path.encoded = "/latest/api/token"
    headers.append(X_AWS_EC2_METADATA_TOKEN_TTL_SECONDS, ttl.toString())
}

fun tokenResponse(ttl: Int, token: String): HttpResponse = HttpResponse(
    HttpStatusCode.OK,
    Headers {
        append(X_AWS_EC2_METADATA_TOKEN_TTL_SECONDS, ttl.toString())
    },
    HttpBody.fromBytes(token.encodeToByteArray()),
)

fun imdsRequest(url: String, token: String): HttpRequest = HttpRequest {
    val parsed = Url.parse(url)
    url(parsed)
    headers.append(X_AWS_EC2_METADATA_TOKEN, token)
}

fun imdsResponse(body: String): HttpResponse = HttpResponse(
    HttpStatusCode.OK,
    Headers.Empty,
    HttpBody.fromBytes(body.encodeToByteArray()),
)
