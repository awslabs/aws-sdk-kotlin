/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package auth.signer

import com.soywiz.klock.DateTime
import com.soywiz.klock.ISO8601.BaseIsoDateTimeFormat
import regions.Region
import types.AwsCredentials
import types.Headers
import types.HttpRequest
import types.QueryParameters
import types.RequestSigningArguments
import types.SdkException
import types.Signer
import types.StringContent
import utils.isUsingStandardPort
import utils.toHexString
import utils.urlEncode

private const val AUTH_HEADER = "authorization"
private const val DATE_HEADER = "date"
private const val X_AMZ_DATE = "x-amz-date"
private const val X_AMZ_SECURITY_TOKEN = "X-Amz-Security-Token";
private const val HOST = "host"
private const val X_AMZ_CONTENT_SHA256 = "x-amz-content-sha256"
private const val AUTHORIZATION = "Authorization"

private const val ALGORITHM_IDENTIFIER = "AWS4-HMAC-SHA256";
private const val UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD"
private const val AWS4_TERMINATOR = "aws4_request"

private val GENERATED_HEADERS = arrayOf(AUTH_HEADER, X_AMZ_DATE, DATE_HEADER)

private val DATE_FORMAT = BaseIsoDateTimeFormat("YYYYMMDD")
private val TIME_FORMAT = BaseIsoDateTimeFormat("YYYYMMDDThhmmssZ")

private val LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE = setOf("connection", "x-amzn-trace-id", "user-agent", "expect")

const val MAX_CACHE_SIZE = 50

// TODO This isn't remotely correct due to not thread safe nor evict possible with Kotlin LinkedHashMap... will need to write our own
private val SIGNING_KEY_CACHE = LinkedHashMap<String, ByteArray>(50)

/**
 * Signer implementation that signs requests with the AWS4 signing protocol.
 */
class AwsSigV4Signer(
    private val service: String,
    private val region: Region,
    private val credentials: AwsCredentials,
    private val urlEncode: Boolean = true,
    private val applyChecksum: Boolean = true
) : Signer {
    private val sha256 by lazy(LazyThreadSafetyMode.NONE) { Sha256() }

    override fun sign(originalRequest: HttpRequest, signingOptions: RequestSigningArguments?): HttpRequest {
        val signingDateTime = signingOptions?.signingDate ?: DateTime.now()
        val request = prepareRequest(originalRequest)

        credentials.sessionToken?.let {
            request.headers[X_AMZ_SECURITY_TOKEN] = it
        }

        addHostHeader(request)
        addDateHeader(request, signingDateTime)

        val contentHash = calculateContentHash(originalRequest)
        if (applyChecksum && !originalRequest.headers.containsKey(X_AMZ_CONTENT_SHA256)) {
            request.headers[X_AMZ_CONTENT_SHA256] = contentHash
        }

        val canonicalRequest = createCanonicalRequest(request, contentHash)
        val scope = generateScope(signingDateTime, service, region.id)
        val stringToSign = createStringToString(canonicalRequest, signingDateTime, scope)

        val signingKey = deriveSigningKey(signingDateTime, credentials)
        val signature = computeSignature(stringToSign, signingKey)

        request.headers[AUTHORIZATION] = buildAuthorizationHeader(scope, signature, credentials, request)

        // TODO: Chunk encoding

        return request
    }

    private fun prepareRequest(request: HttpRequest): HttpRequest {
        val clonedRequest = request.copy(
            headers = request.headers.copy(),
            queryParameters = request.queryParameters?.copy()
        )

        GENERATED_HEADERS.forEach {
            clonedRequest.headers.remove(it)
        }

        return clonedRequest
    }

    private fun addHostHeader(request: HttpRequest) {
        if (request.headers.containsKey(HOST)) {
            return
        }

        // SigV4 requires that we sign the Host header so we have to have it in the request by the time we sign.
        val header = if (!isUsingStandardPort(request.protocol, request.port)) {
            "${request.hostname}:${request.port}"
        } else {
            request.hostname
        }

        request.headers[HOST] = header
    }

    private fun addDateHeader(request: HttpRequest, signingDateTime: DateTime) {
        val formattedDateTime = signingDateTime.format(TIME_FORMAT)
        request.headers[X_AMZ_DATE] = formattedDateTime
    }

    private fun calculateContentHash(request: HttpRequest): String = when (val body = request.body) {
        null -> {
            sha256.update(byteArrayOf())
            sha256.digest().toHexString()
        }
        is StringContent -> {
            sha256.update(body.data.encodeToByteArray())
            sha256.digest().toHexString()
        }
        else -> UNSIGNED_PAYLOAD
    }

    private fun createCanonicalRequest(request: HttpRequest, contentHash: String): String = buildString {
        append(request.method)
        append('\n')
        append(getCanonicalizedResourcePath(request.path))
        append('\n')
        append(getCanonicalizedQueryString(request.queryParameters))
        append('\n')
        append(getCanonicalizedHeaderString(request.headers))
        append('\n')
        append(getSignedHeadersString(request.headers))
        append('\n')
        append(contentHash)
    }

    private fun getCanonicalizedResourcePath(resourcePath: String): String? {
        val value = if (urlEncode) {
            val trimmed = if(resourcePath.elementAtOrNull(0) == '/') {
                resourcePath.drop(1)
            } else {
                resourcePath
            }
            trimmed.urlEncode(true)
        } else {
            resourcePath
        }

        return if (value.startsWith('/')) {
            value
        } else {
            "/$value"
        }
    }

    private fun getCanonicalizedQueryString(queryParameters: QueryParameters?): String {
        queryParameters ?: return ""

        return buildString {
            val sortedQueryKeys = queryParameters.entries.sortedBy { it.key }
            sortedQueryKeys.forEach { queryParameter ->
                val encodedKey = queryParameter.key.urlEncode()

                val sortedParamValues = queryParameter.value.sorted()
                sortedParamValues.forEach { parameterValue ->
                    if (length > 0) {
                        append('&')
                    }
                    append(encodedKey)
                    append('=')
                    append(parameterValue.urlEncode())
                }
            }
        }
    }

    private fun getCanonicalizedHeaderString(headers: Headers): String {
        val sortedHeaders = headers.keys.sortedWith(String.CASE_INSENSITIVE_ORDER)

        return buildString {
            sortedHeaders.forEach { header ->
                if (LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE.contains(header)) {
                    return@forEach
                }

                appendCompactedString(header.toLowerCase())
                append(":")
                appendCompactedString(headers.getValue(header))
                append('\n')
            }
        }
    }

    private fun StringBuilder.appendCompactedString(string: String) {
        var previousIsWhiteSpace = false
        string.forEach {
            previousIsWhiteSpace = if (it.isWhitespace()) {
                if (previousIsWhiteSpace) {
                    return@forEach
                }
                append(' ')
                true
            } else {
                append(it)
                false
            }
        }
    }

    private fun getSignedHeadersString(headers: Headers): String {
        val sortedHeaders = headers.keys.sortedWith(String.CASE_INSENSITIVE_ORDER)

        return buildString {
            sortedHeaders.forEach {
                if (LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE.contains(it)) {
                    return@forEach
                }

                if (length > 0) {
                    append(';')
                }

                append(it.toLowerCase())
            }
        }
    }

    private fun generateScope(dateStamp: DateTime, serviceName: String, regionName: String): String {
        return "${dateStamp.format(DATE_FORMAT)}/$regionName/$serviceName/$AWS4_TERMINATOR"
    }

    private fun createStringToString(
        canonicalRequest: String,
        signingDateTime: DateTime,
        scope: String
    ): String = buildString {
        append(ALGORITHM_IDENTIFIER)
        append('\n')
        append(signingDateTime.format(TIME_FORMAT))
        append('\n')
        append(scope)
        append('\n')
        append(hashData(canonicalRequest.encodeToByteArray()).toHexString())
    }

    private fun deriveSigningKey(
        signingDateTime: DateTime,
        credentials: AwsCredentials
    ): ByteArray {
        val formattedSignDate = signingDateTime.format(DATE_FORMAT)
        val cacheKey = "$formattedSignDate-${credentials.secretAccessKey}-$region-$service"

        SIGNING_KEY_CACHE[cacheKey]?.let {
            return it
        }

        val signingKey = newSigningKey(
            credentials,
            formattedSignDate,
            region.id,
            service
        )

        SIGNING_KEY_CACHE[cacheKey] = signingKey
        SIGNING_KEY_CACHE

        return signingKey
    }

    private fun newSigningKey(
        credentials: AwsCredentials,
        dateStamp: String,
        regionName: String,
        serviceName: String
    ): ByteArray {
        val kSecret = "AWS4${credentials.secretAccessKey}".encodeToByteArray()
        val kDate = sign(dateStamp, kSecret)
        val kRegion = sign(regionName, kDate)
        val kService: ByteArray = sign(serviceName, kRegion)
        return sign(AWS4_TERMINATOR, kService)
    }

    private fun sign(stringData: String, key: ByteArray): ByteArray {
        return try {
            val data = stringData.encodeToByteArray()
            HmacSha256(key).sign(data)
        } catch (e: Exception) {
            throw SdkException("Unable to calculate a request signature: " + e.message, e)
        }
    }

    private fun computeSignature(stringToSign: String, signingKey: ByteArray): ByteArray =
        sign(stringToSign, signingKey)

    private fun buildAuthorizationHeader(
        scope: String,
        signature: ByteArray,
        credentials: AwsCredentials,
        request: HttpRequest
    ): String {
        val signingCredentials = "${credentials.accessKeyId}/$scope"
        val credential = "Credential=$signingCredentials"
        val signerHeaders = "SignedHeaders=${getSignedHeadersString(request.headers)}"
        val signatureHeader = """Signature=${signature.toHexString()}"""

        return "$ALGORITHM_IDENTIFIER $credential, $signerHeaders, $signatureHeader"
    }

    private fun hashData(data: ByteArray): ByteArray {
        sha256.reset()
        sha256.update(data)
        return sha256.digest()
    }
}
