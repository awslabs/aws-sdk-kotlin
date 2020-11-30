package software.aws.kotlinsdk.regions

/**
 * A common interface that all AWS service clients implement as part of their configuration state.
 */
public interface RegionConfig {
    /**
     * AWS Region the client was configured with. Note that this is not always the signing region in the case of global
     * services like IAM.
     */
    public val region: String?

    /**
     * AWS Region to be used for signing the request. This is not always same as [region] in case of global services.
     */
    public val signingRegion: String?
}
