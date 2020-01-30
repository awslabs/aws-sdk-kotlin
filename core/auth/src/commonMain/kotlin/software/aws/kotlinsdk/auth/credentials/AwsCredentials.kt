package software.aws.kotlin.auth.credentials

/**
 * Provides access to the AWS credentials used for accessing AWS services: AWS access key ID and secret access key. These
 * credentials are used to securely sign requests to AWS services.
 *
 * For more details on AWS access keys, see:
 * [
 * http://docs.amazonwebservices.com/AWSSecurityCredentials/1.0/AboutAWSCredentials.html#AccessKeys](http://docs.amazonwebservices.com/AWSSecurityCredentials/1.0/AboutAWSCredentials.html#AccessKeys)
 *
 * @see AwsCredentialsProvider
 */
interface AwsCredentials {
    /**
     * Retrieve the AWS access key, used to identify the user interacting with AWS.
     */
    val accessKeyId: String

    /**
     * Retrieve the AWS secret access key, used to authenticate the user interacting with AWS.
     */
    val secretAccessKey: String

    /**
     * A security or session token to use with these credentials. Usually
     * present for temporary credentials.
     */
    val sessionToken: String?

    /**
     * UNIX epoch timestamp (seconds since 1 January, 1970 00:00:00 GMT) when
     * these credentials will no longer be accepted.
     */
    val expiration: Long? // TODO: klock?
}

//fun AwsCredentials.isAnonymous() = this is AnonymousCredentials