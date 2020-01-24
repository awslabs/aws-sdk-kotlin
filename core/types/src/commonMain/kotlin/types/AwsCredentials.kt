package types

import com.soywiz.klock.DateTime

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
data class AwsCredentials(
    /**
     * Retrieve the AWS access key, used to identify the user interacting with AWS.
     */
    val accessKeyId: String,

    /**
     * Retrieve the AWS secret access key, used to authenticate the user interacting with AWS.
     */
    val secretAccessKey: String,

    /**
     * A security or session token to use with these credentials. Usually
     * present for temporary credentials.
     */
    val sessionToken: String?,

    /**
     * UNIX epoch timestamp (seconds since 1 January, 1970 00:00:00 GMT) when
     * these credentials will no longer be accepted.
     */
    val expiration: DateTime?
) {
    constructor(accessKeyId: String, secretAccessKey: String) : this(accessKeyId, secretAccessKey, null, null)

    override fun toString(): String {
        return "AwsCredentials(accessKeyId='$accessKeyId', expiration=$expiration)"
    }
}

/**
 * Interface for loading [AwsCredentials] that are used for authentication.
 */
interface AwsCredentialsProvider {
    /**
     * Returns [AwsCredentials] that can be used to authorize an AWS request. Each implementation of AWSCredentialsProvider
     * can chose its own strategy for loading credentials. For example, an implementation might load credentials from an existing
     * key management system, or load new credentials when credentials are rotated.
     *
     * If an error occurs during the loading of credentials or credentials could not be found, a runtime exception will be
     * raised.
     *
     * @return AwsCredentials which the caller can use to authorize an AWS request.
     */
    fun resolveCredentials(): AwsCredentials
}