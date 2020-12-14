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
}
