package software.aws.kotlinsdk.regions

/**
 * A common interface that all AWS service clients implement as part of their configuration state.
 */
interface RegionConfig {

    /**
     * AWS Region the client was configured with. Note that this is not always the signing region in the case of global
     * services like IAM.
     * TODO: Define/describe the default region selection heuristic
     */
    val region: String?

    /**
     * AWS Region to be used for signing the request. This is not always same as {@link #AWS_REGION} in case of global services.
     */
    val signingRegion: String?
}
