/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.crt.auth.credentials.build
import aws.sdk.kotlin.runtime.crt.SdkDefaultIO
import aws.sdk.kotlin.crt.auth.credentials.DefaultChainCredentialsProvider as DefaultChainCredentialsProviderCrt

/**
 * Creates the default provider chain used by most AWS SDKs.
 *
 * Generally:
 *
 * (1) Environment
 * (2) Profile
 * (3) (conditional, off by default) ECS
 * (4) (conditional, on by default) EC2 Instance Metadata
 *
 * Support for environmental control of the default provider chain is not yet implemented.
 *
 * @return the newly-constructed credentials provider
 */
public class DefaultChainCredentialsProvider : CrtCredentialsProvider {
    override val crtProvider: DefaultChainCredentialsProviderCrt by lazy {
        DefaultChainCredentialsProviderCrt.build {
            clientBootstrap = SdkDefaultIO.ClientBootstrap
        }
    }
}
