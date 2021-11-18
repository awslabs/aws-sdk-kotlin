/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle

///**
// * A set of specifications for post-processing the generated files (e.g. remove files, move files around, etc)
// */
//class PostProcessSpec {
//
//
//}

class KotlinCodegenProjection(
    /**
     * The name of the projection
     */
    val name: String,

    /**
     * Root directory for this projection
     */
    val projectionRootDir: java.io.File
){

    /**
     * List of files/directories to import when building the projection
     *
     * See https://awslabs.github.io/smithy/1.0/guides/building-models/build-config.html#projections
     */
    var imports: List<String> = emptyList()


    /**
     * Smithy Kotlin plugin settings. This *MUST* be a valid JSON object that conforms to the
     * plugin settings for smithy kotlin.
     *
     * Example:
     * ```json
     * {
     *     "service": <service shape ID>,
     *     "package": {
     *         "name": <generated package name>,
     *         "version": <generated version>
     *         "description": <description>
     *     },
     *     "sdkId": <SDK ID> (Optional: defaults to shape id if not set),
     *     "build": { <build settings> }
     * }
     * ```
     */
    // FIXME - we could make this a typed object if we want or even re-use smithy-kotlin type
    var pluginSettings: String? = null

    //    private var postProcessSpec: PostProcessSpec? = null
    //    fun postProcess(spec: PostProcessSpec.() -> Unit) {
    //        postProcessSpec = PostProcessSpec().apply(spec)
    //    }
}