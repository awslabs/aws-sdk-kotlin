/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.regions.providers

// FIXME - this probably needs to be expect/actual to customize the chain per/target platform (and definitely needs per/platform tests)

/**
 * [AwsRegionProvider] that looks for region in this order:
 *  1. Check `aws.region` system property (JVM only)
 *  2. Check the `AWS_REGION` and `AWS_DEFAULT_REGION` environment variable(s) (JVM, Node, Native)
 *  3. Check the AWS config files/profile for region information
 *  4. If running on EC2, check the EC2 metadata service for region
 */
public class DefaultAwsRegionProviderChain : AwsRegionProviderChain {
    public constructor() : super(EnvironmentRegionProvider())
}
