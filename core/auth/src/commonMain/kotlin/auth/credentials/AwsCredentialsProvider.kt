/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package auth.credentials

/**
 * Interface for loading [AwsCredentials] that are used for authentication.
 *
 * Commonly-used implementations include [StaticCredentialsProvider] for a fixed set of credentials and the
 * [DefaultCredentialsProvider] for discovering credentials from the host's environment. The AWS Security Token
 * Service (STS) client also provides implementations of this interface for loading temporary, limited-privilege credentials from
 * AWS STS.
 */
interface AwsCredentialsProvider {
    /**
     * Returns [AwsCredentials] that can be used to authorize an AWS request. Each implementation of AWSCredentialsProvider
     * can chose its own strategy for loading credentials. For example, an implementation might load credentials from an existing
     * key management system, or load new credentials when credentials are rotated.
     *
     * If an error occurs during the loading of credentials or credentials could not be found, a runtime exception will be
     * raised.
     *
     * @return AwsCredentials which the caller can use to authorize an AWS request.
     */
    fun resolveCredentials(): AwsCredentials
}