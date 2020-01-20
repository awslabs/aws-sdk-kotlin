package auth.credentials

/**
 * A special type of [AwsCredentials] that provides a session token to be used in service authentication. Session
 * tokens are typically provided by a token broker service, like AWS Security Token Service, and provide temporary access to an
 * AWS service.
 */
data class AwsSessionCredentials(
    override val accessKeyId: String,
    override val secretAccessKey: String,
    val sessionToken: String
) : AwsCredentials {
    override fun toString(): String {
        return "AwsSessionCredentials(accessKeyId='$accessKeyId')"
    }
}