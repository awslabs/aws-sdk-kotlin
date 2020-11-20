/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.aws.kotlinsdk.auth

/**
 * Represents a producer/source of AWS credentials
 */
public interface CredentialsProvider {

    /**
     * Request credentials from the provider
     */
    public suspend fun getCredentials(): Credentials
}

// TODO - add proxies for all the credentials providers in aws-crt-kotlin
