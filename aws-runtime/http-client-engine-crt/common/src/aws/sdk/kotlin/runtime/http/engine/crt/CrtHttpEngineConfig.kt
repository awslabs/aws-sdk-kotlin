/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.io.ClientBootstrap
import aws.sdk.kotlin.crt.io.TlsContext
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineConfig
import aws.smithy.kotlin.runtime.util.InternalApi

@InternalApi
public class CrtHttpEngineConfig private constructor(builder: Builder) : HttpClientEngineConfig(builder) {
    public companion object {
        /**
         * The default engine config. Most clients should use this.
         */
        public val Default: CrtHttpEngineConfig = CrtHttpEngineConfig(Builder())
    }

    /**
     * The amount of data that can be buffered before reading from the socket will cease. Reading will
     * resume as data is consumed.
     */
    public val initialWindowSizeBytes: Int = builder.initialWindowSizeBytes

    /**
     * The [ClientBootstrap] to use for the engine. By default it is a shared instance.
     */
    public var clientBootstrap: ClientBootstrap? = builder.clientBootstrap

    /**
     * The TLS context to use. By default it is a shared instance.
     */
    public var tlsContext: TlsContext? = builder.tlsContext

    public class Builder : HttpClientEngineConfig.Builder() {
        /**
         * Set the amount of data that can be buffered before reading from the socket will cease. Reading will
         * resume as data is consumed.
         */
        public var initialWindowSizeBytes: Int = DEFAULT_WINDOW_SIZE_BYTES

        /**
         * Set the [ClientBootstrap] to use for the engine. By default it is a shared instance.
         */
        public var clientBootstrap: ClientBootstrap? = null

        /**
         * Set the TLS context to use. By default it is a shared instance.
         */
        public var tlsContext: TlsContext? = null

        internal fun build(): CrtHttpEngineConfig = CrtHttpEngineConfig(this)
    }
}
