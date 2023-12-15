/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Returns true if the service is S3
 */
val ServiceShape.isS3: Boolean
    get() = sdkId.lowercase() == "s3"

/**
 * Returns true if the service is S3 Control
 */
val ServiceShape.isS3Control: Boolean
    get() = sdkId.lowercase() == "s3 control"
