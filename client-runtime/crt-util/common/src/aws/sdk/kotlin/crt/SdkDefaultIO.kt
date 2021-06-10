/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt

import aws.sdk.kotlin.crt.io.ClientBootstrap
import aws.sdk.kotlin.crt.io.EventLoopGroup
import aws.sdk.kotlin.crt.io.HostResolver
import aws.sdk.kotlin.runtime.InternalSdkApi

// FIXME - this should default to number of processors
private const val DEFAULT_EVENT_LOOP_THREAD_COUNT: Int = 1

/**
 * Default (CRT) IO used by the SDK when not configured manually/directly
 */
@InternalSdkApi
public object SdkDefaultIO {
    /**
     * The default event loop group to run IO on
     */
    public val EventLoop: EventLoopGroup by lazy {
        // TODO - can we register shutdown in appropriate runtimes (e.g. jvm: addShutdown, native: atexit(), etc) when/if these lazy block(s) run?
        EventLoopGroup(DEFAULT_EVENT_LOOP_THREAD_COUNT)
    }

    /**
     * The default host resolver to resolve DNS queries with
     */
    public val HostResolver: HostResolver by lazy {
        HostResolver(EventLoop)
    }

    /**
     * The default client bootstrap
     */
    public val ClientBootstrap: ClientBootstrap by lazy {
        ClientBootstrap(EventLoop, HostResolver)
    }
}
