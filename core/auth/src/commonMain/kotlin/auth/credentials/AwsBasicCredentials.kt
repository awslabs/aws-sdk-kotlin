package auth.credentials

/**
 * Provides access to the AWS credentials used for accessing AWS services: AWS access key ID and secret access key. These
 * credentials are used to securely sign requests to AWS services.
 *
 *
 * For more details on AWS access keys, see:
 * [
 * http://docs.amazonwebservices.com/AWSSecurityCredentials/1.0/AboutAWSCredentials.html#AccessKeys](http://docs.amazonwebservices.com/AWSSecurityCredentials/1.0/AboutAWSCredentials.html#AccessKeys)
 *
 * @see AwsCredentialsProvider
 */
data class AwsBasicCredentials(
    override val accessKeyId: String,
    override val secretAccessKey: String
) : AwsCredentials {
    override fun toString(): String {
        return "AwsBasicCredentials(accessKeyId='$accessKeyId')"
    }
}