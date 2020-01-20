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
package auth.signer.params

import auth.credentials.AwsCredentials
import regions.Region
import kotlin.time.Clock

/**
 * Parameters that are used during signing.
 *
 * Required parameters vary based on signer implementations. Signer implementations might only use a
 * subset of params in this class.
 */
open class Aws4SignerParams(
    private val awsCredentials: AwsCredentials,
    private val signingName: String,
    private val signingRegion: Region,
    private val doubleUrlEncode: Boolean = true,
    private val timeOffset: Int?,
    private val signingClockOverride: Clock?
)