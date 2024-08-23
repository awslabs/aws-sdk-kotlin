/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.Converter

/**
 * Creates a [Converter] which transforms between [Set] and [List] instances (both of some type [T])
 * @param T The type of elements in the [Set]/[List]
 */
public inline fun <reified T> setToListConverter(): Converter<Set<T>, List<T>> =
    Converter(Set<T>::toList, List<T>::toSet)
