/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.auth.credentials.internal.sso.SsoClient
import aws.sdk.kotlin.runtime.config.profile.normalizePath
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.serde.json.*
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.*

/**
 * [CredentialsProvider] that uses AWS Single Sign-On (AWS SSO) to source credentials. The
 * provider is expected to be configured for the AWS Region where the AWS SSO user portal is hosted.
 *
 * The provider does not initiate or perform the AWS SSO login flow. It is expected that you have
 * already performed the SSO login flow using (e.g. using the AWS CLI `aws sso login`). The provider
 * expects a valid non-expired access token for the AWS SSO user portal URL in `~/.aws/sso/cache`.
 * If a cached token is not found, it is expired, or the file is malformed an exception will be thrown.
 *
 *
 * **Instantiating AWS SSO provider directly**
 *
 * You can programmatically construct the AWS SSO provider in your application, and provide the necessary
 * information to load and retrieve temporary credentials using an access token from `~/.aws/sso/cache`.
 *
 * ```
 * val source = SsoCredentialsProvider(
 *     accountId = "123456789",
 *     roleName = "SsoReadOnlyRole",
 *     startUrl = "https://my-sso-portal.awsapps.com/start",
 *     ssoRegion = "us-east-2"
 * )
 *
 * // Wrap the provider with a caching provider to cache the credentials until their expiration time
 * val ssoProvider = CachedCredentialsProvider(source)
 * ```
 * It is important that you wrap the provider with [CachedCredentialsProvider] if you are programatically constructing
 * the provider directly. This prevents your application from accessing the cached access token and requesting new
 * credentials each time the provider is used to source credentials.
 *
 *
 * **Additional Resources**
 * * [Configuring the AWS CLI to use AWS Single Sign-On](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-sso.html)
 * * [AWS Single Sign-On User Guide](https://docs.aws.amazon.com/singlesignon/latest/userguide/what-is.html)
 */
public class SsoCredentialsProvider public constructor(
    /**
     * The AWS account ID that temporary AWS credentials will be resolved for
     */
    public val accountId: String,

    /**
     * The IAM role in the AWS account that temporary AWS credentials will be resolved for
     */
    public val roleName: String,

    /**
     * The start URL (also known as the "User Portal URL") provided by the SSO service
     */
    public val startUrl: String,

    /**
     * The AWS region where the SSO directory for the given [startUrl] is hosted.
     */
    public val ssoRegion: String,

    /**
     * The [HttpClientEngine] to use when making requests to the AWS SSO service
     */
    private val httpClientEngine: HttpClientEngine? = null,

    /**
     * The platform provider
     */
    private val platformProvider: PlatformProvider = Platform,

    /**
     * The source of time for the provider
     */
    private val clock: Clock = Clock.System

) : CredentialsProvider {

    override suspend fun getCredentials(): Credentials {

        val token = loadTokenFile()

        val client = SsoClient {
            region = ssoRegion
            httpClientEngine = this@SsoCredentialsProvider.httpClientEngine
        }

        val resp = try {
            client.getRoleCredentials {
                accountId = this@SsoCredentialsProvider.accountId
                roleName = this@SsoCredentialsProvider.roleName
                accessToken = token.accessToken
            }
        } catch (ex: Exception) {
            throw CredentialsNotLoadedException("GetRoleCredentials operation failed", ex)
        } finally {
            client.close()
        }

        val roleCredentials = resp.roleCredentials ?: throw CredentialsProviderException("Expected SSO roleCredentials to not be null")

        return Credentials(
            accessKeyId = checkNotNull(roleCredentials.accessKeyId) { "Expected accessKeyId in SSO roleCredentials response" },
            secretAccessKey = checkNotNull(roleCredentials.secretAccessKey) { "Expected secretAccessKey in SSO roleCredentials response" },
            sessionToken = roleCredentials.sessionToken,
            expiration = Instant.fromEpochSeconds(roleCredentials.expiration),
            "SSO"
        )
    }

    private suspend fun loadTokenFile(): SsoToken {
        val key = getCacheFilename(startUrl)
        val bytes = with(platformProvider) {
            val defaultCacheLocation = normalizePath(filepath("~", ".aws", "sso", "cache"), this)
            readFileOrNull(filepath(defaultCacheLocation, key))
        } ?: throw ProviderConfigurationException("Invalid or missing SSO session cache. Run `aws sso login` to initiate a new SSO session")

        val token = deserializeSsoToken(bytes)
        val now = clock.now()
        if (now > token.expiresAt) throw ProviderConfigurationException("The SSO session has expired. To refresh this SSO session run `aws sso login` with the corresponding profile.")

        return token
    }
}

internal fun PlatformProvider.filepath(vararg parts: String): String = parts.joinToString(separator = filePathSeparator)

internal fun getCacheFilename(url: String): String {
    val sha1HexDigest = url.encodeToByteArray().sha1().encodeToHex()
    return "$sha1HexDigest.json"
}

internal data class SsoToken(
    val accessToken: String,
    val expiresAt: Instant,
    val region: String? = null,
    val startUrl: String? = null
)

internal fun deserializeSsoToken(json: ByteArray): SsoToken {
    val lexer = jsonStreamReader(json)

    var accessToken: String? = null
    var expiresAtRfc3339: String? = null
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
        region,
        startUrl
    )
}

/**
 * An error associated with a cached SSO token from `~/.aws/sso/cache/`
 */
public class InvalidSsoTokenException(message: String, cause: Throwable? = null) : ConfigurationException(message, cause)
