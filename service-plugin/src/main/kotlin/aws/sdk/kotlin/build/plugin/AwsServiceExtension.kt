/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.build.plugin

import org.gradle.api.Project

open class AwsServiceExtension(val project: Project) {
    // FIXME - we won't be able to do it this way, we will have to rely on convention because to configure
    // dependencies dynamically it needs to already know where the model file is
//    var model: String? = null
}
