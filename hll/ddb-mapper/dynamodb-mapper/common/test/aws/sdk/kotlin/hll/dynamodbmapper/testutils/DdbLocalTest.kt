/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.testutils

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.deleteTable
import aws.sdk.kotlin.services.dynamodb.waiters.waitUntilTableNotExists
import aws.smithy.kotlin.runtime.net.Host
import aws.smithy.kotlin.runtime.net.Scheme
import aws.smithy.kotlin.runtime.net.url.Url
import io.kotest.core.spec.style.AnnotationSpec
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val DDB_LOCAL_PORT = 44212 // Keep in sync with build.gradle.kts

abstract class DdbLocalTest : AnnotationSpec() {
    companion object {
        private const val DOUBLE_TOLERANCE = 0.000001

        fun assertEquals(expected: Double, actual: Double?) {
            assertNotNull(actual)
            assertEquals(expected, actual, DOUBLE_TOLERANCE)
        }
    }

    private val ddbHolder = lazy {
        DynamoDbClient {
            endpointUrl = Url {
                scheme = Scheme.HTTP
                host = Host.Domain("localhost")
                port = DDB_LOCAL_PORT
            }

            region = "us-west-2" // FIXME

            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "DUMMY"
                secretAccessKey = "DUMMY"
            }
        }
    }
    val ddb by ddbHolder

    private val tempTables = mutableListOf<String>()

    suspend fun createTable(name: String, schema: ItemSchema.PartitionKey<*, *>, vararg items: Map<String, Any>) =
        createTable(name, listOf(schema.partitionKey), items.toList())

    suspend fun createTable(name: String, schema: ItemSchema.CompositeKey<*, *, *>, vararg items: Map<String, Any>) =
        createTable(name, listOf(schema.partitionKey, schema.sortKey), items.toList())

    private suspend fun createTable(name: String, keys: List<KeySpec<*>>, items: List<Map<String, Any>>) {
        ddb.createTable(name, keys)
        tempTables += name
        ddb.putItems(name, items)
    }

    fun mapper(config: DynamoDbMapper.Config.Builder.() -> Unit = { }) = DynamoDbMapper(ddb, config)

    @AfterAll
    fun cleanUp() {
        if (ddbHolder.isInitialized()) {
            runBlocking {
                tempTables.forEach { name ->
                    ddb.deleteTable { tableName = name }
                    ddb.waitUntilTableNotExists { tableName = name }
                }
            }

            ddb.close()
        }
    }
}
