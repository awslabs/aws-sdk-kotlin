/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.io.MutableBuffer
import software.aws.clientrt.io.SdkBuffer
import software.aws.clientrt.io.readAvailable

internal actual fun transferRequestBody(outgoing: SdkBuffer, dest: MutableBuffer) {
    outgoing.readAvailable(dest.buffer)
}
