package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbItem
public data class RenamedPartitionKey(
    @DynamoDbPartitionKey
    @DynamoDbAttribute("user_id")
    var id: Int,

    @DynamoDbAttribute("fName") var givenName: String,
    @DynamoDbAttribute("lName") var surname: String,
    var age: Int,
)
