/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.sqs

import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Returns true if the service is Sqs
 */
val ServiceShape.isSqs: Boolean
    get() = sdkId.lowercase() == "sqs"
