/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.auth.credentials.internal.ssooidc.SsoOidcClient
import aws.sdk.kotlin.runtime.auth.credentials.internal.ssooidc.createToken
import aws.sdk.kotlin.runtime.auth.credentials.internal.ssooidc.model.CreateTokenResponse
import aws.sdk.kotlin.runtime.config.profile.normalizePath
import aws.smithy.kotlin.runtime.hashing.sha1
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.identity.Token
import aws.smithy.kotlin.runtime.identity.TokenProvider
import aws.smithy.kotlin.runtime.serde.json.*
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.tracing.debug
import aws.smithy.kotlin.runtime.tracing.traceSpan
import aws.smithy.kotlin.runtime.util.*
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_SSO_TOKEN_REFRESH_BUFFER_SECONDS = 60 * 5
private const val OIDC_GRANT_TYPE_REFRESH = "refresh_token"

/**
 *
 * @param ssoSessionName the name of the SSO Session from the shared config file to load tokens for
 * @param startUrl the start URL (also known as the "User Portal URL") provided by the SSO service
 * @param ssoRegion the AWS region where the SSO directory for the given [startUrl] is hosted.
 * @param refreshBufferWindow amount of time before the actual credential expiration time when credentials are
 * considered expired. For example, if credentials are expiring in 15 minutes, and the buffer time is 10 seconds,
 * then any requests made after 14 minutes and 50 seconds will load new credentials. Defaults to 10 seconds.
 * @param httpClientEngine the [HttpClientEngine] to use when making requests to the AWS SSO service
 * @param platformProvider the platform provider to use
 * @param clock the source of time for the provider
 */
public class SsoTokenProvider(
    public val ssoSessionName: String,
    public val startUrl: String,
    public val ssoRegion: String,
    public val refreshBufferWindow: Duration = DEFAULT_SSO_TOKEN_REFRESH_BUFFER_SECONDS.seconds,
    private val httpClientEngine: HttpClientEngine? = null,
    private val platformProvider: PlatformProvider = PlatformProvider.System,
    private val clock: Clock = Clock.System,
) : TokenProvider {

    // debounce concurrent requests for a token
    private val sfg = SingleFlightGroup<SsoToken>()

    override suspend fun resolve(attributes: Attributes): Token = sfg.singleFlight {
        getToken(attributes)
    }

    private suspend fun getToken(attributes: Attributes): SsoToken {
        val token = readTokenFromCache(ssoSessionName, platformProvider)
        if (clock.now() < (token.expiresAt - refreshBufferWindow)) {
            coroutineContext.traceSpan.debug<SsoTokenProvider> { "using cashed token for sso-session: $ssoSessionName" }
            return token
        }

        // token is within expiry window
        if (token.canRefresh) {
            // TODO - async refresh in background (configurable?)
            return attemptRefresh(token)
        }

        return token.takeIf { clock.now() < it.expiresAt } ?: throwTokenExpired()
    }
    private suspend fun attemptRefresh(oldToken: SsoToken): SsoToken {
        coroutineContext.traceSpan.debug<SsoTokenProvider> { "attempting to refresh token for sso-session: $ssoSessionName" }
        val result = runCatching { refreshToken(oldToken) }
        return result
            .onSuccess { refreshed -> writeToken(refreshed) }
            .getOrElse { cause ->
                if (clock.now() >= oldToken.expiresAt) {
                    throwTokenExpired(cause)
                }
                coroutineContext.traceSpan.debug<SsoTokenProvider> { "refresh token failed, original token is still valid until ${oldToken.expiresAt} for sso-session: $ssoSessionName, re-using" }
                oldToken
            }
    }

    private suspend fun writeToken(refreshed: SsoToken) {
        val cacheKey = getCacheFilename(ssoSessionName)
        val filepath = normalizePath(platformProvider.filepath("~", ".aws", "sso", "cache", cacheKey), platformProvider)
        try {
            val contents = serializeSsoToken(refreshed)
            platformProvider.writeFile(filepath, contents)
        } catch (ex: Exception) {
            coroutineContext.traceSpan.debug<SsoTokenProvider>(ex) { "failed to write refreshed token back to disk at $filepath" }
        }
    }

    private fun throwTokenExpired(cause: Throwable? = null): Nothing {
        throw InvalidSsoTokenException("SSO token for sso-session: $ssoSessionName is expired", cause)
    }

    private suspend fun refreshToken(oldToken: SsoToken): SsoToken {
        val client = SsoOidcClient {
            region = ssoRegion
            httpClientEngine = this@SsoTokenProvider.httpClientEngine
            // FIXME - create an anonymous credential provider to explicitly avoid default chain creation
            // FIXME - technically it shouldn't need a cred provider because the only operation uses anonymous auth, investigate why
            // we are still pulling that integration in.
        }

        val resp = client.createToken {
            clientId = oldToken.clientId
            clientSecret = oldToken.clientSecret
            refreshToken = oldToken.refreshToken
            grantType = OIDC_GRANT_TYPE_REFRESH
        }

        return resp.toSsoToken(oldToken, clock)
    }
}

internal fun PlatformProvider.filepath(vararg parts: String): String = parts.joinToString(separator = filePathSeparator)
internal suspend fun readTokenFromCache(cacheKey: String, platformProvider: PlatformProvider): SsoToken {
    val key = getCacheFilename(cacheKey)
    val bytes = with(platformProvider) {
        val defaultCacheLocation = normalizePath(filepath("~", ".aws", "sso", "cache"), this)
        readFileOrNull(filepath(defaultCacheLocation, key))
    } ?: throw ProviderConfigurationException("Invalid or missing SSO session cache. Run `aws sso login` to initiate a new SSO session")
    return deserializeSsoToken(bytes)
}
internal fun getCacheFilename(cacheKey: String): String {
    val sha1HexDigest = cacheKey.encodeToByteArray().sha1().encodeToHex()
    return "$sha1HexDigest.json"
}

internal data class SsoToken(
    val accessToken: String,
    val expiresAt: Instant,
    val refreshToken: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val registrationExpiresAt: Instant? = null,
    val region: String? = null,
    val startUrl: String? = null,
) : Token {

    override val attributes: Attributes = emptyAttributes()
    override val token: String
        get() = accessToken

    override val expiration: Instant
        get() = expiresAt
}

private fun CreateTokenResponse.toSsoToken(oldToken: SsoToken, clock: Clock): SsoToken {
    val token = checkNotNull(accessToken) { "missing accessToken from CreateTokenResponse" }
    val expiresAt = clock.now() + expiresIn.seconds
    return oldToken.copy(accessToken = token, expiresAt = expiresAt, refreshToken = refreshToken)
}

/**
 * Test if a token has the components to allow it to be refreshed for a new one
 */
private val SsoToken.canRefresh: Boolean
    get() = clientId != null && clientSecret != null && refreshToken != null

internal fun deserializeSsoToken(json: ByteArray): SsoToken {
    val lexer = jsonStreamReader(json)

    var accessToken: String? = null
    var expiresAtRfc3339: String? = null
    var refreshToken: String? = null
    var clientId: String? = null
    var clientSecret: String? = null
    var registrationExpiresAt: Instant? = null
    var region: String? = null
    var startUrl: String? = null

    try {
        lexer.nextTokenOf<JsonToken.BeginObject>()
        loop@while (true) {
            when (val token = lexer.nextToken()) {
                is JsonToken.EndObject -> break@loop
                is JsonToken.Name -> when (token.value) {
                    "accessToken" -> accessToken = lexer.nextTokenOf<JsonToken.String>().value
                    "expiresAt" -> expiresAtRfc3339 = lexer.nextTokenOf<JsonToken.String>().value
                    "refreshToken" -> refreshToken = lexer.nextTokenOf<JsonToken.String>().value
                    "clientId" -> clientId = lexer.nextTokenOf<JsonToken.String>().value
                    "clientSecret" -> clientSecret = lexer.nextTokenOf<JsonToken.String>().value
                    "registrationExpiresAt" -> registrationExpiresAt = lexer.nextTokenOf<JsonToken.String>().value.let { Instant.fromIso8601(it) }
                    "region" -> region = lexer.nextTokenOf<JsonToken.String>().value
                    "startUrl" -> startUrl = lexer.nextTokenOf<JsonToken.String>().value
                    else -> lexer.skipNext()
                }
                else -> error("expected either key or end of object")
            }
        }
    } catch (ex: Exception) {
        throw InvalidSsoTokenException("invalid cached SSO token", ex)
    }

    if (accessToken == null) throw InvalidSsoTokenException("missing `accessToken`")
    val expiresAt = expiresAtRfc3339?.let { Instant.fromIso8601(it) } ?: throw InvalidSsoTokenException("missing `expiresAt`")

    return SsoToken(
        accessToken,
        expiresAt,
        refreshToken,
        clientId,
        clientSecret,
        registrationExpiresAt,
        region,
        startUrl,
    )
}

internal fun serializeSsoToken(token: SsoToken): ByteArray =
    jsonStreamWriter(pretty = true).apply {
        beginObject()
        writeName("accessToken")
        writeValue(token.accessToken)
        writeName("expiresAt")
        writeValue(token.expiresAt.format(TimestampFormat.ISO_8601))
        writeNotNull("refreshToken", token.refreshToken)
        writeNotNull("clientId", token.clientId)
        writeNotNull("clientSecret", token.clientSecret)
        writeNotNull("registrationExpiresAt", token.registrationExpiresAt?.let { it.format(TimestampFormat.ISO_8601) })
        writeNotNull("region", token.region)
        writeNotNull("startUrl", token.startUrl)
        endObject()
    }.bytes ?: error("serializing SsoToken failed")

private fun JsonStreamWriter.writeNotNull(name: String, value: String?) {
    if (value == null) return
    writeName(name)
    writeValue(value)
}

/**
 * An error associated with a cached SSO token from `~/.aws/sso/cache/`
 */
public class InvalidSsoTokenException(message: String, cause: Throwable? = null) : ConfigurationException(message, cause)
