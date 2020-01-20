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
package auth.signer

import annotations.SdkProtectedApi
import auth.credentials.AwsCredentials
import core.interceptor.ExecutionAttribute
import core.interceptor.ExecutionInterceptor
import core.interceptor.SdkExecutionAttribute
import core.signer.Signer
import regions.Region
import java.io.IOException
import kotlin.jvm.Throws

/**
 * AWS-specific signing attributes attached to the execution. This information is available to [ExecutionInterceptor]s and
 * [Signer]s.
 */
object AwsSignerExecutionAttribute : SdkExecutionAttribute {
    /**
     * The key under which the request credentials are set.
     */
    val AWS_CREDENTIALS: ExecutionAttribute<AwsCredentials> = ExecutionAttribute("AwsCredentials")

    /**
     * The AWS [Region] that is used for signing a request. This is not always same as the region configured on the client
     * for global services like IAM.
     */
    val SIGNING_REGION: ExecutionAttribute<Region> = ExecutionAttribute("SigningRegion")

    /**
     * The signing name of the service to be using in SigV4 signing
     */
    val SERVICE_SIGNING_NAME: ExecutionAttribute<String> = ExecutionAttribute("ServiceSigningName")

    /**
     * The key to specify whether to use double url encoding during signing.
     */
    val SIGNER_DOUBLE_URL_ENCODE: ExecutionAttribute<Boolean> = ExecutionAttribute("DoubleUrlEncode")

    /**
     * The key to specify the expiration time when pre-signing aws requests.
     */
    val PRESIGNER_EXPIRATION: ExecutionAttribute<java.time.Instant> = ExecutionAttribute("PresignerExpiration")
}