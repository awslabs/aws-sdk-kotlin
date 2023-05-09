/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.middleware

import aws.smithy.kotlin.runtime.http.SdkHttpClient
import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.httptest.TestEngine
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategyOptions
import aws.smithy.kotlin.runtime.retries.delay.DelayProvider
import aws.smithy.kotlin.runtime.retries.delay.StandardRetryTokenBucket
import aws.smithy.kotlin.runtime.retries.delay.StandardRetryTokenBucketOptions
import aws.smithy.kotlin.runtime.util.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AwsRetryHeaderMiddlewareTest {
    private val client = SdkHttpClient(TestEngine())

    @Test
    fun testItSetsRetryHeaders() = runTest {
        // see retry-header SEP
        val op = SdkHttpOperation.build<Unit, Unit> {
            serializer = UnitSerializer
            deserializer = UnitDeserializer
            context {
                // required operation context
                operationName = "TestOperation"
            }
        }

        val delayProvider = DelayProvider { }
        val strategy = StandardRetryStrategy(
            StandardRetryStrategyOptions.Default,
            StandardRetryTokenBucket(StandardRetryTokenBucketOptions.Default),
            delayProvider,
        )
        val maxAttempts = strategy.options.maxAttempts

        op.install(AwsRetryHeaderMiddleware())
        op.roundTrip(client, Unit)

        val calls = op.context.attributes[HttpOperationContext.HttpCallList]
        val sdkRequestId = op.context.sdkRequestId

        assertTrue(calls.all { it.request.headers[AMZ_SDK_INVOCATION_ID_HEADER] == sdkRequestId })
        calls.forEachIndexed { idx, call ->
            assertEquals("attempt=${idx + 1}; max=$maxAttempts", call.request.headers[AMZ_SDK_REQUEST_HEADER])
        }
    }
}
