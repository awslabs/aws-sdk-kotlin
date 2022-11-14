/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.smithy.kotlin.runtime.hashing.Crc32
import aws.smithy.kotlin.runtime.io.SdkSink
import aws.smithy.kotlin.runtime.io.SdkSource
import aws.smithy.kotlin.runtime.io.internal.SdkSinkObserver
import aws.smithy.kotlin.runtime.io.internal.SdkSourceObserver

internal class CrcSource(source: SdkSource) : SdkSourceObserver(source) {
    private val _crc = Crc32()

    val crc: UInt
        get() = _crc.digestValue()

    override fun observe(data: ByteArray, offset: Int, length: Int) {
        _crc.update(data, offset, length)
    }
}

internal class CrcSink(sink: SdkSink) : SdkSinkObserver(sink) {
    private val _crc = Crc32()

    val crc: UInt
        get() = _crc.digestValue()

    override fun observe(data: ByteArray, offset: Int, length: Int) {
        _crc.update(data, offset, length)
    }
}
