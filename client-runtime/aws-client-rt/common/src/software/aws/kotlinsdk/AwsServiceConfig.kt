package software.aws.kotlinsdk

/**
 * A common interface that all AWS service clients implement as part of their configuration state.
 */
interface AwsServiceConfig {

    /**
     * Specifies the AWS region that AWS service requests should be directed to. If not provided a default is used.
     * TODO: Define/describe the default region selection heuristic
     */
    val region: String?

    /**
     * Specifies zero or more credential providers that will be called to resolve credentials before making
     * AWS service calls.  If not provided a default credential provider chain is used.
     * TODO: Define/describe the default credential provider chain
     */
    val credentialProviders: AwsCredentialsProviders?
}