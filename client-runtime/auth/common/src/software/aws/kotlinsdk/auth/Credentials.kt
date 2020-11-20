package software.aws.kotlinsdk.auth

/**
 * Represents a set of AWS credentials
 */
public data class Credentials(val accessKeyId: String, val secretAccessKey: String, val sessionToken: String?)
