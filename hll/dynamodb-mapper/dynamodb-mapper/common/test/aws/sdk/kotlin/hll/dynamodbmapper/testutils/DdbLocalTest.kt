/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.testutils

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
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

/**
 * The base class for test classes which need DynamoDB Local running. This class provides a few convenience declarations
 * for subclasses:
 * * [ddb] — An instance of a low-level [DynamoDbClient] utilizing the DynamoDB Local instance which may be used for
 *   setting up or verifying various mapper tests
 * * [createTable] — Creates a table for the given name/schema and optionally persists the given items into it. All
 *   tables created via this method will be automatically dropped when the test spec completes (whether successfully or
 *   unsuccessfully).
 * * [mapper] — Returns a [DynamoDbMapper] instance utilizing the DynamoDB Local instance
 */
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

            region = "DUMMY"

            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "DUMMY"
                secretAccessKey = "DUMMY"
            }
        }
    }

    /**
     * An instance of a low-level [DynamoDbClient] utilizing the DynamoDB Local instance which may be used for setting
     * up or verifying various mapper tests. If this is the first time accessing the value, the client will be
     * initialized.
     */
    val ddb by ddbHolder

    private val tempTables = mutableListOf<String>()

    /**
     * Creates a table for the given name/schema and optionally persists the given items into it. All tables created via
     * this method will be automatically dropped when the test spec completes (whether successfully or unsuccessfully).
     * @param name The name of the table to create
     * @param schema The schema for the table. This is used to derive the primary key and to map the [items] (if any).
     * @param items A collection of maps of strings to values which will be mapped and persisted to the new table before
     * returning. This can be used to pre-populate a table for a test.
     */
    suspend fun createTable(name: String, schema: ItemSchema<*>, vararg items: Item) =
        createTable(name, schema, mapOf(), mapOf(), items.toList())

    /**
     * Creates a table for the given name/schema and optionally persists the given items into it. All tables created via
     * this method will be automatically dropped when the test spec completes (whether successfully or unsuccessfully).
     * @param name The name of the table to create
     * @param schema The schema for the table. This is used to derive the primary key and to map the [items] (if any).
     * @param gsis A map of GSI names to schemas
     * @param lsis A map of LSI names to schemas
     * @param items A collection of maps of strings to values which will be mapped and persisted to the new table before
     * returning. This can be used to pre-populate a table for a test.
     */
    suspend fun createTable(
        name: String,
        schema: ItemSchema<*>,
        gsis: Map<String, ItemSchema<*>>,
        lsis: Map<String, ItemSchema<*>>,
        items: List<Item>,
    ) {
        ddb.createTable(name, schema, gsis, lsis)
        tempTables += name
        ddb.putItems(name, items)
    }

    /**
     * Returns a [DynamoDbMapper] instance utilizing the DynamoDB Local instance
     * @param config A function to set the configuration of the mapper before it's built
     */
    fun mapper(
        ddb: DynamoDbClient? = null,
        config: DynamoDbMapper.Config.Builder.() -> Unit = { },
    ) = DynamoDbMapper(ddb ?: this.ddb, config)

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
