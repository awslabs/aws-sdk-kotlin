/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.testutils

import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.batchWriteItem
import aws.sdk.kotlin.services.dynamodb.createTable
import aws.sdk.kotlin.services.dynamodb.getItem
import aws.sdk.kotlin.services.dynamodb.model.*
import aws.sdk.kotlin.services.dynamodb.waiters.waitUntilTableExists

/**
 * Creates a table with the given name/keys. This method waits for the asynchronous completion of the operation before
 * returning.
 * @param name The name for the new table
 * @param keys A list of [KeySpec] describing the components of the primary key
 */
suspend fun DynamoDbClient.createTable(name: String, keys: List<KeySpec<*>>) {
    createTable {
        tableName = name
        attributeDefinitions = keys.map { key ->
            AttributeDefinition {
                attributeName = key.name
                attributeType = key.toScalarAttributeType()
            }
        }
        keySchema = keys.mapIndexed { index, key ->
            KeySchemaElement {
                attributeName = key.name
                keyType = if (index == 0) KeyType.Hash else KeyType.Range
            }
        }
        provisionedThroughput {
            // provisioned throughput is required but ignored by DDB Local so just use dummy values
            readCapacityUnits = 1
            writeCapacityUnits = 1
        }
    }

    waitUntilTableExists { tableName = name }
}

/**
 * Gets an item from the given table with the given keys
 * @param tableName The name of the table to fetch from
 * @param keys One or more tuples of string key to value
 */
suspend fun DynamoDbClient.getItem(tableName: String, vararg keys: Pair<String, Any>) = getItem {
    this.tableName = tableName
    key = ddbItem(keys.toMap())
}

/**
 * Puts the given items into the given table
 * @param tableName The name of the table to insert to
 * @param items A collection of maps of strings to values to be mapped and persisted to the table
 */
suspend fun DynamoDbClient.putItems(tableName: String, items: List<Map<String, Any>>) {
    val writeRequests = items.map { mapItem ->
        WriteRequest {
            putRequest {
                item = ddbItem(mapItem)
            }
        }
    }

    if (writeRequests.isNotEmpty()) {
        batchWriteItem {
            requestItems = mapOf(tableName to writeRequests)
        }
    }
}

/**
 * Converts a map of strings to values to a map of strings to [AttributeValue]
 * @param item The item to convert
 */
fun ddbItem(item: Map<String, Any>) = item.mapValues { (_, v) ->
    when (v) {
        is ByteArray -> AttributeValue.B(v)
        is Number -> AttributeValue.N(v.toString())
        is String -> AttributeValue.S(v)
        else -> TODO("Implement support for ${v::class} types!")
    }
}

/**
 * Converts a collection of tuples of strings to values to a map of strings to [AttributeValue]
 * @param attributes The attributes to convert
 */
fun ddbItem(vararg attributes: Pair<String, Any>) = ddbItem(attributes.toMap())

private fun KeySpec<*>.toScalarAttributeType() = when (this) {
    is KeySpec.ByteArray -> ScalarAttributeType.B
    is KeySpec.Number -> ScalarAttributeType.N
    is KeySpec.String -> ScalarAttributeType.S
}
