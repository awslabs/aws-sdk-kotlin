package software.aws.kotlin.auth.credentials

/**
 * An implementation of [AwsCredentialsProvider] that returns a set implementation of [AwsCredentials].
 */
class StaticCredentialsProvider private constructor(private val credentials: AwsCredentials) :
    AwsCredentialsProvider {
    override fun resolveCredentials(): AwsCredentials {
        return credentials
    }

    companion object {
        /**
         * Create a credentials provider that always returns the provided set of credentials.
         */
        fun create(credentials: AwsCredentials): StaticCredentialsProvider {
            return StaticCredentialsProvider(credentials)
        }
    }
}