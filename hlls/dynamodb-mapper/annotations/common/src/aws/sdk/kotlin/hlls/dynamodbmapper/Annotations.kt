/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hlls.dynamodbmapper

@Target(AnnotationTarget.PROPERTY)
public annotation class DdbAttribute(val name: String)

@Target(AnnotationTarget.CLASS)
public annotation class DdbItem

@Target(AnnotationTarget.PROPERTY)
public annotation class DdbPartitionKey

@Target(AnnotationTarget.PROPERTY)
public annotation class DdbSortKey
