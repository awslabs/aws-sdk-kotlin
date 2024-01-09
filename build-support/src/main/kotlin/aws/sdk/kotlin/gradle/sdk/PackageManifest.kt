/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk

import kotlinx.serialization.Serializable

/**
 * Manifest containing additional metadata about services.
 */
@Serializable
data class PackageManifest(
    val packages: List<PackageMetadata>,
)

/**
 * Validate the package manifest for errors throwing an exception if any exist.
 */
fun PackageManifest.validate() {
    val distinct = mutableMapOf<String, PackageMetadata>()
    val errors = mutableListOf<String>()
    packages.forEach {
        val existing = distinct[it.sdkId]
        if (existing != null) {
            errors.add("multiple packages with same sdkId `${it.sdkId}`: first: $existing; second: $it")
        }
        distinct[it.sdkId] = it
    }

    check(errors.isEmpty()) { errors.joinToString(separator = "\n") }
}

@Serializable
data class PackageMetadata(
    public val sdkId: String,
    public val namespace: String,
    public val artifactName: String,
    public val brazilName: String,
) {
    companion object {

        /**
         * Create a new [PackageMetadata] from inferring values using the given sdkId
         */
        fun from(sdkId: String): PackageMetadata =
            PackageMetadata(
                sdkId,
                packageNamespaceForService(sdkId),
                sdkIdToArtifactName(sdkId),
                sdkIdToBrazilName(sdkId),
            )
    }
}
