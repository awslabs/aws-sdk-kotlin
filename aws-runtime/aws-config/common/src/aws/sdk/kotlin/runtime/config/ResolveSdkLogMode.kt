/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.client.LogMode
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Attempt to resolve the SDK log mode used to make requests
 */
@InternalSdkApi
public fun resolveLogMode(
    platformProvider: PlatformProvider = PlatformProvider.System,
): LogMode = AwsSdkSetting.LogMode.resolve(platformProvider)!!
