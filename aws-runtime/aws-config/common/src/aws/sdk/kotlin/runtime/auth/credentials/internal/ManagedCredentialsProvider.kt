/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.io.SharedCloseable
import aws.smithy.kotlin.runtime.io.SharedCloseableImpl

private class ManagedCredentialsProvider(
    private val managed: CredentialsProvider,
) : CredentialsProvider by managed {
    private val wrapped = SharedCloseableImpl(managed)

    override fun share() { wrapped.share() }

    override fun close() { wrapped.close() }
}

/**
 * Wraps a [CredentialsProvider] to implement [SharedCloseable] for tracking internal use across multiple clients.
 */
@InternalSdkApi
public fun CredentialsProvider.manage(): CredentialsProvider =
    if (this is ManagedCredentialsProvider) this else ManagedCredentialsProvider(this)

/**
 * Extension to check whether a [CredentialsProvider] is managed.
 */
@InternalSdkApi
public fun CredentialsProvider.isManaged(): Boolean = this is ManagedCredentialsProvider
