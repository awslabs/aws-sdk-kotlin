/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.identity.CloseableTokenProvider
import aws.smithy.kotlin.runtime.identity.TokenProvider
import aws.smithy.kotlin.runtime.io.SdkManagedCloseable

private class ManagedTokenProvider(
    private val delegate: CloseableTokenProvider,
) : SdkManagedCloseable(delegate), CloseableTokenProvider by delegate

/**
 * Wraps a [TokenProvider] for internal runtime management by the SDK.
 */
@InternalApi
public fun CloseableTokenProvider.manage(): TokenProvider =
    if (this is ManagedTokenProvider) this else ManagedTokenProvider(this)
