/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.InternalApi
import java.net.InetAddress

/**
 * Wrapper for java's InetAddress
 */
@InternalApi
public actual class Ip<InternalRepresentation> private actual constructor(private val address: InternalRepresentation) {
    public actual companion object {
        public actual fun getAllFromHost(host: String): List<Ip<Any>> =
            InetAddress.getAllByName(host).map {
                Ip(it)
            }
    }
    public actual fun isLoopbackAddress(): Boolean =
        (address as InetAddress).isLoopbackAddress
}
