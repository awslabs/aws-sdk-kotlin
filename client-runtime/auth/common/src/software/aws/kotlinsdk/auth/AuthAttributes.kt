/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.aws.kotlinsdk.auth

import software.aws.clientrt.util.AttributeKey

/**
 * Operation attributes related to auth
 */
public object AuthAttributes {
    /**
     * Mark a request as unsigned
     */
    public val UnsignedRequest: AttributeKey<Boolean> = AttributeKey("UnsignedRequest")
}
