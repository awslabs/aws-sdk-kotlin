/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.InternalApi

/**
 * Wrapper for native implementations of IP addresses / DNS host resolving
 */
@InternalApi
public actual class Ip<InternalRepresentation> private actual constructor(private val address: InternalRepresentation) {
    public actual companion object {
        public actual fun getAllFromHost(host: String): List<Ip<Any>> = TODO("Not yet implemented")
    }
    public actual fun isLoopbackAddress(): Boolean = TODO("Not yet implemented")
}
