/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.operation

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.client.ExecutionContext
import aws.smithy.kotlin.runtime.util.AttributeKey

/**
 * Operation context element for adding additional metadata to the `User-Agent` header string.
 *
 * Access via extension property [ExecutionContext.customUserAgentMetadata]
 */
public class CustomUserAgentMetadata {
    internal val extras: MutableMap<String, String> = mutableMapOf()
    internal val typedExtras: MutableList<TypedUserAgentMetadata> = mutableListOf()

    internal companion object {
        public val ContextKey: AttributeKey<CustomUserAgentMetadata> = AttributeKey("CustomUserAgentMetadata")
    }

    /**
     * Add additional key-value pairs of metadata to the request. These will show up as `md/key/value` when sent.
     */
    public fun add(key: String, value: String) {
        extras[key] = value
    }

    @InternalSdkApi
    public fun add(metadata: TypedUserAgentMetadata) {
        typedExtras.add(metadata)
    }
}

/**
 * Get the [CustomUserAgentMetadata] instance to append additional context to the generated `User-Agent` string.
 */
public val ExecutionContext.customUserAgentMetadata: CustomUserAgentMetadata
    get() = computeIfAbsent(CustomUserAgentMetadata.ContextKey) { CustomUserAgentMetadata() }

/**
 * Marker interface for addition of classified metadata types (e.g. [ConfigMetadata] or [FeatureMetadata]).
 */
@InternalSdkApi
public sealed interface TypedUserAgentMetadata

/**
 * Feature metadata
 * @property name The name of the feature
 * @property version Optional version of the feature (if independently versioned)
 */
@InternalSdkApi
public data class FeatureMetadata(val name: String, val version: String? = null) : TypedUserAgentMetadata {
    override fun toString(): String = if (version != null) "ft/$name/$version" else "ft/$name"
}

/**
 * Configuration metadata
 * @property name The configuration property name (e.g. "retry-mode")
 * @property value The property value (e.g. "standard")
 */
@InternalSdkApi
public data class ConfigMetadata(val name: String, val value: String) : TypedUserAgentMetadata {
    override fun toString(): String = when (value.lowercase()) {
        "true" -> "cfg/$name"
        else -> "cfg/$name/$value"
    }
}
