/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.signing

import aws.sdk.kotlin.runtime.auth.credentials.Credentials
import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider
import aws.sdk.kotlin.runtime.execution.AuthAttributes
import aws.smithy.kotlin.runtime.client.ExecutionContext
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.http.response.HttpCall
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.http.util.splitAsQueryParameters
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.get
import io.ktor.http.cio.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

private const val DEFAULT_SIGNING_ISO_DATE = "2015-08-30T12:36:00Z"

private val DefaultTestSigningConfig = AwsSigningConfig.Builder().apply {
    algorithm = AwsSigningAlgorithm.SIGV4
    credentials = Credentials(
        accessKeyId = "AKIDEXAMPLE",
        secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
    )

    date = Instant.fromIso8601(DEFAULT_SIGNING_ISO_DATE)
    region = "us-east-1"
    service = "service"
    useDoubleUriEncode = true
    normalizeUriPath = true
}

data class Sigv4TestSuiteTest(
    val path: String,
    val request: HttpRequestBuilder,
    val signedRequest: HttpRequestBuilder,
    val config: AwsSigningConfig = DefaultTestSigningConfig.build()
)

// FIXME - move to common test (will require ability to access test resources in a KMP compatible way)
class Sigv4TestSuite {

    private val testSuiteDir: File
        get() {
            val url = this::class.java.classLoader.getResource("aws-signing-test-suite/v4") ?: error("failed to load sigv4 test suite resource")
            return Paths.get(url.toURI()).toFile()
        }

    private val disabledTests = setOf(
        // ktor-http-cio parser doesn't support parsing multiline headers since they are deprecated in RFC7230
        "get-header-value-multiline",
        // ktor fails to parse with space in it (expects it to be a valid request already encoded)
        "get-space-normalized",
        "get-space-unnormalized",

        // no signed request to test against
        "get-vanilla-query-order-key",
        "get-vanilla-query-order-value",

        // FIXME - crt-java has utf8 bug when converting request,
        // re-enable after https://github.com/awslabs/aws-crt-java/pull/419 is merged
        "get-vanilla-utf8-query",

        // fixme - revisit why this fails
        "get-utf8"
    )

    // get all directories with a request.txt file in it
    private val testDirs = testSuiteDir
        .walkTopDown()
        .filter { !it.isDirectory && it.name == "request.txt" }
        .filterNot { it.parentFile.name in disabledTests }
        // .filter{ it.parentFile.name == "get-vanilla-query-order-key-case" }
        .map { it.parent }

    @Test
    fun testParseRequest() {
        // sanity test that we are converting requests from file correctly
        val noBodyTest = testSuiteDir.resolve("post-vanilla").path
        val actual = getSignedRequest(noBodyTest)

        assertEquals(3, actual.headers.names().size)
        assertIs<HttpBody.Empty>(actual.body)
        assertEquals("example.amazonaws.com", actual.headers["Host"])
        assertEquals("20150830T123600Z", actual.headers["X-Amz-Date"])
        assertEquals(
            "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, SignedHeaders=host;x-amz-date, Signature=5da7c1a2acd57cee7505fc6676e4e544621c30862966e37dddb68e92efbe5d6b",
            actual.headers["Authorization"]
        )
    }

    @Test
    fun testSigv4TestSuiteHeaders() {
        assertTrue(testSuiteDir.isDirectory)

        val tests = testDirs.map { dir ->
            try {
                val req = getRequest(dir)
                val sreq = getSignedRequest(dir)
                val config = getSigningConfig(dir) ?: DefaultTestSigningConfig
                Sigv4TestSuiteTest(dir, req, sreq, config.build())
            } catch (ex: Exception) {
                println("failed to get request from $dir: ${ex.message}")
                throw ex
            }
        }

        testSigv4Middleware(tests)
    }

    @Test
    fun testSigv4TestSuiteQuery() {
        assertTrue(testSuiteDir.isDirectory)

        val tests = testDirs.map { dir ->
            try {
                val req = getRequest(dir)
                val sreq = getQuerySignedRequest(dir)
                val config = getSigningConfig(dir) ?: DefaultTestSigningConfig
                config.signatureType = AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS
                Sigv4TestSuiteTest(dir, req, sreq, config.build())
            } catch (ex: Exception) {
                println("failed to get request from $dir: ${ex.message}")
                throw ex
            }
        }

        testSigv4Middleware(tests)
    }

    /**
     * Run the test suite against the AwsSigv4Middleware implementation
     */
    private fun testSigv4Middleware(tests: Sequence<Sigv4TestSuiteTest>): Unit = runBlocking {
        tests.forEach { test ->
            // println("running sigv4 middleware test for: ${test.path}")
            try {
                val op = buildOperation(test.config, test.request)
                val actual = getSignedRequest(test.config, op)
                assertRequestsEqual(test.signedRequest.build(), actual, "actual signed request for ${test.path} not equal")
            } catch (ex: Exception) {
                println("failed to get a signed request for ${test.path}: $ex")
                throw ex
            }
        }
    }

    private fun assertRequestsEqual(expected: HttpRequest, actual: HttpRequest, message: String? = null) {
        assertEquals(expected.method, actual.method, message)
        assertEquals(expected.url.path, actual.url.path, message)

        expected.headers.forEach { key, values ->
            val expectedValues = values.sorted().joinToString(separator = ", ")
            val actualValues = actual.headers.getAll(key)?.sorted()?.joinToString(separator = ", ")
            assertNotNull(actualValues, "expected header key `$key` not found in actual signed request")
            assertEquals(expectedValues, actualValues, "expected header `$key=$expectedValues` in signed request")
        }

        expected.url.parameters.forEach { key, values ->
            val expectedValues = values.sorted().joinToString(separator = ", ")
            val actualValues = actual.url.parameters.getAll(key)?.sorted()?.joinToString(separator = ", ")
            assertNotNull(actualValues, "expected query key `$key` not found in actual signed request")
            assertEquals(expectedValues, actualValues, "expected query param `$key=$expectedValues` in signed request")
        }

        val expectedBody = expected.body
        when (expectedBody) {
            is HttpBody.Empty -> assertIs<HttpBody.Empty>(actual.body)
            is HttpBody.Bytes -> {
                val actualBody = assertIs<HttpBody.Bytes>(actual.body)
                assertContentEquals(expectedBody.bytes(), actualBody.bytes())
            }
            else -> TODO("body comparsion not implemented")
        }
    }

    /**
     * Parse context.json if it exists into a signing config
     */
    @OptIn(ExperimentalTime::class)
    private fun getSigningConfig(dir: String): AwsSigningConfig.Builder? {
        val file = Paths.get(dir, "context.json")
        if (!file.exists()) return null
        val json = Json.parseToJsonElement(file.readText()).jsonObject
        val creds = json["credentials"]!!.jsonObject
        val config = AwsSigningConfig.Builder()
        config.credentials = Credentials(
            accessKeyId = creds["access_key_id"]!!.jsonPrimitive.content,
            secretAccessKey = creds["secret_access_key"]!!.jsonPrimitive.content,
            sessionToken = creds["token"]?.jsonPrimitive?.content
        )
        config.region = json["region"]!!.jsonPrimitive.content
        config.service = json["service"]!!.jsonPrimitive.content

        json["expiration_in_seconds"]?.jsonPrimitive?.int?.let {
            config.expiresAfter = Duration.seconds(it)
        }

        json["normalize"]?.jsonPrimitive?.boolean?.let {
            config.normalizeUriPath = it
        }

        val isoDate = json["timestamp"]?.jsonPrimitive?.content ?: DEFAULT_SIGNING_ISO_DATE
        config.date = Instant.fromIso8601(isoDate)

        json["omit_session_token"]?.jsonPrimitive?.boolean?.let {
            config.omitSessionToken = it
        }

        val sbht = json["sign_body"]?.jsonPrimitive?.booleanOrNull ?: false
        // https://github.com/awslabs/aws-c-auth/blob/main/tests/sigv4_signing_tests.c#L566
        if (sbht) {
            config.signedBodyHeader = AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256
        }

        return config
    }

    /**
     * Get `request.txt` from the given directory [dir]
     */
    private fun getRequest(dir: String): HttpRequestBuilder {
        val file = Paths.get(dir, "request.txt").toFile()
        return parseRequestFromFile(file)
    }

    /**
     * Get `header-signed-request.txt` from the given directory [dir]
     */
    private fun getSignedRequest(dir: String): HttpRequestBuilder {
        val file = Paths.get(dir, "header-signed-request.txt").toFile()
        return parseRequestFromFile(file)
    }

    /**
     * Get `query-signed-request.txt` from the given directory [dir]
     */
    private fun getQuerySignedRequest(dir: String): HttpRequestBuilder {
        val file = Paths.get(dir, "query-signed-request.txt").toFile()
        return parseRequestFromFile(file)
    }

    /**
     * Parse a file containing an HTTP request into an in memory representation of an SDK request
     */
    @OptIn(InternalAPI::class)
    private fun parseRequestFromFile(file: File): HttpRequestBuilder {
        // we have to do some massaging of these input files to get a valid request out of the parser.
        var text = file.readText()
        val lines = text.lines()
        val hasBody = lines.last() != "" && lines.find { it == "" } != null

        // in particular the parser requires the headers section to have two trailing newlines (\r\n)
        if (!hasBody) {
            text = text.trimEnd() + "\r\n\r\n"
        }

        val chan = ByteReadChannel(text.encodeToByteArray())

        val parsed = runBlocking {
            parseRequest(chan) ?: error("failed to parse http request from: $file")
        }

        val builder = HttpRequestBuilder()
        builder.method = when (parsed.method.value.uppercase()) {
            "GET" -> HttpMethod.GET
            "POST" -> HttpMethod.POST
            else -> TODO("HTTP method ${parsed.method} not implemented")
        }

        builder.url.path = parsed.parsePath()
        parsed.parseQueryParameters()?.let {
            builder.url.parameters.appendAll(it)
        }

        val parsedHeaders = CIOHeaders(parsed.headers)
        parsedHeaders.forEach { key, values ->
            builder.headers.appendAll(key, values)
        }

        if (hasBody) {
            val bytes = runBlocking { chan.readRemaining().readBytes() }
            builder.body = ByteArrayContent(bytes)
        }

        return builder
    }

    private fun getFileWithExtension(dir: String, ext: String): File {
        // by convention all the files have the same name as their parent directory
        val base = Paths.get(dir).name
        val f = Paths.get(dir, "$base.$ext").toFile()
        assertTrue(f.exists(), "$f does not exist!")
        return f
    }
}

/**
 * parse query params (if any) from ktor request uri
 */
private fun Request.parseQueryParameters(): QueryParameters? {
    val idx = uri.indexOf("?")
    if (idx < 0 || idx + 1 > uri.length) return null

    val fragmentIdx = uri.indexOf("#", startIndex = idx)
    val rawQueryString = if (fragmentIdx > 0) uri.substring(idx + 1, fragmentIdx) else uri.substring(idx + 1)
    return rawQueryString.splitAsQueryParameters()
}

/**
 * parse path from ktor request uri
 */
private fun Request.parsePath(): String {
    val idx = uri.indexOf("?")
    return if (idx > 0) uri.substring(0, idx) else uri.toString()
}

/**
 * Construct on SdkHttpOperation for testing with middleware
 *
 * @param config The signing config to use to set operation context attributes
 * @param serialized The parsed HTTP request that represents the serialized version of some request/operation
 */
private fun buildOperation(
    config: AwsSigningConfig,
    serialized: HttpRequestBuilder
): SdkHttpOperation<Unit, HttpResponse> = SdkHttpOperation.build {
    serializer = object : HttpSerialize<Unit> {
        override suspend fun serialize(context: ExecutionContext, input: Unit): HttpRequestBuilder = serialized
    }
    deserializer = IdentityDeserializer

    context {
        operationName = "testSigningOperation"
        service = config.service
        set(AuthAttributes.SigningRegion, config.region)
        config.date?.let {
            set(AuthAttributes.SigningDate, it)
        }
        set(AuthAttributes.SigningService, config.service)
    }
}

/**
 * Get the actual signed request after sending it through middleware
 *
 * @param config The signing config to use when creating the middleware
 * @param operation The operation to sign
 */
@OptIn(ExperimentalTime::class)
private suspend fun getSignedRequest(
    config: AwsSigningConfig,
    operation: SdkHttpOperation<Unit, HttpResponse>
): HttpRequest {
    val mockEngine = object : HttpClientEngineBase("test") {
        override suspend fun roundTrip(request: HttpRequest): HttpCall {
            val now = Instant.now()
            val resp = HttpResponse(HttpStatusCode.fromValue(200), Headers.Empty, HttpBody.Empty)
            return HttpCall(request, resp, now, now)
        }
    }
    val client = sdkHttpClient(mockEngine)

    operation.install(AwsSigV4SigningMiddleware) {
        credentialsProvider = if (config.credentialsProvider != null) {
            config.credentialsProvider
        } else {
            val creds = assertNotNull(config.credentials, "credentials or credentialsProvider must be set for test")
            object : CredentialsProvider {
                override suspend fun getCredentials(): Credentials = creds
            }
        }
        signingService = config.service
        useDoubleUriEncode = config.useDoubleUriEncode
        normalizeUriPath = config.normalizeUriPath
        omitSessionToken = config.omitSessionToken
        signedBodyHeaderType = config.signedBodyHeaderType
        signatureType = config.signatureType
        expiresAfter = config.expiresAfter
    }

    operation.roundTrip(client, Unit)
    return operation.context[HttpOperationContext.HttpCallList].last().request
}
