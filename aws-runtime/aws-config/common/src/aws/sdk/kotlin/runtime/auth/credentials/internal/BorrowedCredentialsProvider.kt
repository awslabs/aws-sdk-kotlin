/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.io.Closeable

private class BorrowedCredentialsProvider(
    private val borrowed: CredentialsProvider
) : CredentialsProvider by borrowed, Closeable {
    override fun close() { }
}

/**
 * Wraps another [CredentialsProvider] with a no-op close implementation. This inserts a level of indirection for
 * use cases when a provider is explicitly given to the SDK, and its ownership should remain with the caller.
 * This allows the SDK to treat resources as owned and not have to track ownership state.
 */
@InternalSdkApi
public fun CredentialsProvider.borrow(): CredentialsProvider = when (this) {
    is BorrowedCredentialsProvider -> this
    else -> BorrowedCredentialsProvider(this)
}
