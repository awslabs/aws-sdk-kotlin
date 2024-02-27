/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper

import aws.sdk.kotlin.hll.dynamodbmapper.schemas.ItemSchema
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient

// TODO refactor to interface, add support for multi-table operations, document, add unit tests
public class DynamoDbMapper(public val client: DynamoDbClient) {
    public fun <I, PK> getTable(
        name: String,
        schema: ItemSchema.PartitionKey<I, PK>,
    ): Table.PartitionKey<I, PK> = Table(client, name, schema)

    public fun <I, PK, SK> getTable(
        name: String,
        schema: ItemSchema.CompositeKey<I, PK, SK>,
    ): Table.CompositeKey<I, PK, SK> = Table(client, name, schema)
}
