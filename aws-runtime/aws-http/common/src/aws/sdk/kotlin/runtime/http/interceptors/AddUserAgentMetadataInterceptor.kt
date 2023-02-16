/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.interceptors

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.http.operation.CustomUserAgentMetadata
import aws.smithy.kotlin.runtime.client.RequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor

/**
 * Adds metadata to the user agent sent in requests.
 * @param metadata A map of keys/values to add to existing metadata.
 */
@InternalSdkApi
public class AddUserAgentMetadataInterceptor(private val metadata: Map<String, String>) : HttpInterceptor {
    override fun readBeforeExecution(context: RequestInterceptorContext<Any>) {
        val existing = context.executionContext.getOrNull(CustomUserAgentMetadata.ContextKey)

        val new = existing ?: CustomUserAgentMetadata()
        metadata.forEach { (k, v) -> new.add(k, v) }

        context.executionContext[CustomUserAgentMetadata.ContextKey] = new
    }
}
