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
package software.aws.kotlin.auth.signer.params

import software.aws.kotlin.auth.credentials.AwsCredentials
import software.aws.kotlin.regions.Region
import kotlin.time.Clock

class AwsS3V4SignerParams(
    awsCredentials: AwsCredentials,
    signingName: String,
    signingRegion: Region,
    doubleUrlEncode: Boolean = true,
    timeOffset: Int?,
    signingClockOverride: Clock?,
    val enableChunkedEncoding: Boolean = false,
    val enablePayloadSigning: Boolean = false
) : Aws4SignerParams(awsCredentials, signingName, signingRegion, doubleUrlEncode, timeOffset, signingClockOverride)