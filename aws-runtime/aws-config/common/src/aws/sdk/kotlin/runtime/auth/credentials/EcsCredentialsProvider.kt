/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.AwsSdkSetting.AwsContainerCredentialsRelativeUri
import aws.sdk.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.ServiceException
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.client.ExecutionContext
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.endpoints.Endpoint
import aws.smithy.kotlin.runtime.http.engine.DefaultHttpEngine
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.http.middleware.ResolveEndpoint
import aws.smithy.kotlin.runtime.http.middleware.Retry
import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.http.request.header
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.logging.Logger
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategyOptions
import aws.smithy.kotlin.runtime.retries.delay.ExponentialBackoffWithJitter
import aws.smithy.kotlin.runtime.retries.delay.ExponentialBackoffWithJitterOptions
import aws.smithy.kotlin.runtime.retries.delay.StandardRetryTokenBucket
import aws.smithy.kotlin.runtime.retries.delay.StandardRetryTokenBucketOptions
import aws.smithy.kotlin.runtime.retries.policy.RetryDirective
import aws.smithy.kotlin.runtime.retries.policy.RetryErrorType
import aws.smithy.kotlin.runtime.retries.policy.RetryPolicy
import aws.smithy.kotlin.runtime.serde.json.JsonDeserializer
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.util.Platform
import aws.smithy.kotlin.runtime.util.PlatformEnvironProvider

/**
 * The elastic container metadata service endpoint that should be called by the [aws.sdk.kotlin.runtime.auth.credentials.EcsCredentialsProvider]
 * when loading data from the container metadata service.
 *
 * This is not used if the [AwsContainerCredentialsRelativeUri] is not specified.
 */
private const val AWS_CONTAINER_SERVICE_ENDPOINT = "http://169.254.170.2"

private const val PROVIDER_NAME = "EcsContainer"

/**
 * A [CredentialsProvider] that sources credentials from a local metadata service.
 *
 * This provider is frequently used with an AWS-provided credentials service such as Amazon Container Service (ECS).
 * However, it is possible to use environment variables to configure this provider to use any local metadata service.
 *
 * For more information on configuring ECS credentials see [IAM Roles for tasks](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html)
 *
 * @param platformProvider the platform provider
 * @param httpClientEngine the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 *
 */
public class EcsCredentialsProvider internal constructor(
    private val platformProvider: PlatformEnvironProvider,
    httpClientEngine: HttpClientEngine? = null,
) : CredentialsProvider, Closeable {

    public constructor() : this(Platform)

    private val manageEngine = httpClientEngine == null
    private val httpClientEngine = httpClientEngine ?: DefaultHttpEngine()

    private val retryMiddleware = run {
        val tokenBucket = StandardRetryTokenBucket(StandardRetryTokenBucketOptions.Default)
        val delayProvider = ExponentialBackoffWithJitter(ExponentialBackoffWithJitterOptions.Default)
        val strategy = StandardRetryStrategy(StandardRetryStrategyOptions.Default, tokenBucket, delayProvider)
        val policy = EcsCredentialsRetryPolicy()
        Retry<Credentials>(strategy, policy)
    }

    override suspend fun getCredentials(): Credentials {
        val logger = Logger.getLogger<EcsCredentialsProvider>()
        val authToken = AwsSdkSetting.AwsContainerAuthorizationToken.resolve(platformProvider)
        val relativeUri = AwsSdkSetting.AwsContainerCredentialsRelativeUri.resolve(platformProvider)
        val fullUri = AwsSdkSetting.AwsContainerCredentialsFullUri.resolve(platformProvider)

        val url = when {
            relativeUri?.isBlank() == false -> validateRelativeUri(relativeUri)
            fullUri?.isBlank() == false -> validateFullUri(fullUri)
            else -> throw ProviderConfigurationException("Container credentials URI not set")
        }

        val op = SdkHttpOperation.build<Unit, Credentials> {
            serializer = EcsCredentialsSerializer(authToken)
            deserializer = EcsCredentialsDeserializer()
            context {
                operationName = "EcsCredentialsProvider"
                service = "n/a"
            }
        }

        op.install(ResolveEndpoint(resolver = { Endpoint(url) }))
        op.install(retryMiddleware)

        logger.debug { "retrieving container credentials" }
        val client = sdkHttpClient(httpClientEngine, manageEngine = false)
        val creds = try {
            op.roundTrip(client, Unit)
        } catch (ex: Exception) {
            logger.debug { "failed to obtain credentials from container metadata service" }
            throw when (ex) {
                is CredentialsProviderException -> ex
                else -> CredentialsProviderException("Failed to get credentials from container metadata service", ex)
            }
        } finally {
            client.close()
        }

        logger.debug { "obtained credentials from container metadata service; expiration=${creds.expiration?.format(TimestampFormat.ISO_8601)}" }

        return creds
    }

    /**
     * Validate that the [relativeUri] can be combined with the static ECS endpoint to form a valid URL
     */
    private fun validateRelativeUri(relativeUri: String): Url = try {
        Url.parse("${AWS_CONTAINER_SERVICE_ENDPOINT}$relativeUri")
    } catch (ex: Exception) {
        throw ProviderConfigurationException("Invalid relativeUri `$relativeUri`", ex)
    }

    /**
     * Validate that [uri] is valid to be used as a full provider URI
     *
     * Either:
     * 1. The URL uses `https
     * 2. The URL refers to a loopback device. If a URL contains a domain name instead of an IP address a DNS lookup
     * will be performed. ALL resolved IP addresses MUST refer to a loopback interface.
     *
     * @return the validated URL
     */
    private suspend fun validateFullUri(uri: String): Url {
        // full URI requires verification either https OR that the host resolves to loopback device
        val url = try {
            Url.parse(uri)
        } catch (ex: Exception) {
            throw ProviderConfigurationException("Invalid fullUri `$uri`", ex)
        }

        if (url.scheme == Protocol.HTTPS) return url

        // TODO - validate loopback via DNS resolution instead of fixed set. Custom host names (including localhost) that
        //  resolve to loopback won't work until then. ALL resolved addresses MUST resolve to the loopback device
        val allowedHosts = setOf("127.0.0.1", "[::1]")

        if (url.host !in allowedHosts) {
            throw ProviderConfigurationException(
                "The container credentials full URI ($uri) has an invalid host. Host can only be one of [${allowedHosts.joinToString()}].",
            )
        }
        return url
    }

    override fun close() {
        if (manageEngine) {
            httpClientEngine.close()
        }
    }
}

private class EcsCredentialsDeserializer : HttpDeserialize<Credentials> {
    override suspend fun deserialize(context: ExecutionContext, response: HttpResponse): Credentials {
        val payload = response.body.readAll() ?: throw CredentialsProviderException("HTTP credentials response did not contain a payload")
        val deserializer = JsonDeserializer(payload)
        return when (val resp = deserializeJsonCredentials(deserializer)) {
            is JsonCredentialsResponse.SessionCredentials -> Credentials(
                resp.accessKeyId,
                resp.secretAccessKey,
                resp.sessionToken,
                resp.expiration,
                PROVIDER_NAME,
            )
            is JsonCredentialsResponse.Error -> throw CredentialsProviderException("Error retrieving credentials from container service: code=${resp.code}; message=${resp.message}")
        }
    }
}

private class EcsCredentialsSerializer(
    private val authToken: String? = null,
) : HttpSerialize<Unit> {
    override suspend fun serialize(context: ExecutionContext, input: Unit): HttpRequestBuilder {
        val builder = HttpRequestBuilder()
        builder.url.path
        builder.header("Accept", "application/json")
        builder.header("Accept-Encoding", "identity")
        if (authToken != null) {
            builder.header("Authorization", authToken)
        }
        return builder
    }
}

internal class EcsCredentialsRetryPolicy : RetryPolicy<Any?> {
    override fun evaluate(result: Result<Any?>): RetryDirective = when {
        result.isSuccess -> RetryDirective.TerminateAndSucceed
        else -> evaluate(result.exceptionOrNull()!!)
    }

    private fun evaluate(throwable: Throwable): RetryDirective = when (throwable) {
        is ServiceException -> {
            val httpResp = throwable.sdkErrorMetadata.protocolResponse as? HttpResponse
            val status = httpResp?.status
            if (status?.category() == HttpStatusCode.Category.SERVER_ERROR) {
                RetryDirective.RetryError(RetryErrorType.ServerSide)
            } else {
                RetryDirective.TerminateAndFail
            }
        }
        else -> RetryDirective.TerminateAndFail
    }
}
