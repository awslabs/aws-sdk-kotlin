/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hlls.dynamodbmapper.values

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

public interface ValueConverter<A> {
    public fun fromAv(attr: AttributeValue): A
    public fun toAv(value: A): AttributeValue
}
