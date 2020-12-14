package software.aws.kotlinsdk.auth

/**
 * A common interface that all AWS service clients implement as part of their configuration state.
 */
public interface AuthConfig {

    /**
     * Specifies zero or more credential providers that will be called to resolve credentials before making
     * AWS service calls.  If not provided a default credential provider chain is used.
     */
    public val credentialsProvider: CredentialsProvider?

    /**
     * AWS Region to be used for signing the request. This is not always same as `region` in case of global services.
     */
    public val signingRegion: String?
}
