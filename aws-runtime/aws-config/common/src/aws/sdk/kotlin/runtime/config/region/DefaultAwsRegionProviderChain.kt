/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.region

import aws.sdk.kotlin.runtime.region.AwsRegionProvider

/**
 * [AwsRegionProvider] that looks for region in this order:
 *  1. Check `aws.region` system property (JVM only)
 *  2. Check the `AWS_REGION` environment variable (JVM, Node, Native)
 *  3. Check the AWS config files/profile for region information
 *  4. If running on EC2, check the EC2 metadata service for region
 */
public expect class DefaultAwsRegionProviderChain public constructor() : AwsRegionProvider
