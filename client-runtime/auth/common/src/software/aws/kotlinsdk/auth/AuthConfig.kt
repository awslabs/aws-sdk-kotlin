package software.aws.kotlinsdk.auth

/**
 * A common interface that all AWS service clients implement as part of their configuration state.
 */
interface AuthConfig {
    /**
     * Specifies zero or more credential providers that will be called to resolve credentials before making
     * AWS service calls.  If not provided a default credential provider chain is used.
     * TODO: Define/describe the default credential provider chain
     */
    val credentialProviders: AwsCredentialsProviders?
}
