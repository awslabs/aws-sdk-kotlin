/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.io.ManagedCloseable

@InternalSdkApi
public class ManagedCredentialsProvider(
    private val delegate: CredentialsProvider,
) : ManagedCloseable(delegate), CredentialsProvider by delegate {
    override fun close() { super<ManagedCloseable>.close() }
}

/**
 * Wraps a [CredentialsProvider] to track shared use across clients.
 */
@InternalSdkApi
public fun CredentialsProvider.manage(): CredentialsProvider =
    if (this is ManagedCredentialsProvider) this else ManagedCredentialsProvider(this)
