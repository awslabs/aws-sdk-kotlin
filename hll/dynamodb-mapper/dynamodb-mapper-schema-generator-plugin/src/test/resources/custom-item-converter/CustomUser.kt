package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import my.custom.item.converter.MyCustomUserConverter

@DynamoDbItem(converter = MyCustomUserConverter::class)
public data class CustomUser(
    @DynamoDbPartitionKey var id: Int = 1,
    var givenName: String = "Johnny",
    var surname: String = "Appleseed",
    var age: Int = 0,
)
