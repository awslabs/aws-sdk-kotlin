package my.custom.item.converter

import aws.sdk.kotlin.hll.dynamodbmapper.items.AttributeDescriptor
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.items.SimpleItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.IntConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringConverter
import org.example.CustomUser

public object MyCustomUserConverter : ItemConverter<CustomUser> by SimpleItemConverter(
    builderFactory = { CustomUser() },
    build = { this },
    descriptors = arrayOf(
        AttributeDescriptor(
            "id",
            CustomUser::id,
            CustomUser::id::set,
            IntConverter,
        ),
        AttributeDescriptor(
            "myCustomFirstName",
            CustomUser::givenName,
            CustomUser::givenName::set,
            StringConverter,
        ),
        AttributeDescriptor(
            "myCustomLastName",
            CustomUser::surname,
            CustomUser::surname::set,
            StringConverter,
        ),
        AttributeDescriptor(
            "myCustomAge",
            CustomUser::age,
            CustomUser::age::set,
            IntConverter,
        ),
    ),
)
