package aws.sdk.kotlin.runtime.config

/**
 * The properties and name of the active AWS configuration profile.
 *
 * @property profileName active profile
 * @property properties key/value pairs of properties specified by the active profile, accessible via [Map<K, V>]
 */
data class AwsConfiguration(
    val profileName: String,
    private val properties: Map<String, String>
) : Map<String, String> by properties

// Standard cross-SDK properties

/**
 * The AWS signing and endpoint region to use for a profile
 */
public val AwsConfiguration.region: String?
    get() = this["region"]

/**
 * The identifier that specifies the entity making the request for a profile
 */
public val AwsConfiguration.awsAccessKeyId: String?
    get() = this["aws_access_key_id"]

/**
 * The credentials that authenticate the entity specified by the access key
 */
public val AwsConfiguration.awsSecretAccessKey: String?
    get() = this["aws_secret_access_key"]

/**
 * A semi-temporary session token that authenticates the entity is allowed to access a specific set of resources
 */
public val AwsConfiguration.awsSessionToken: String?
    get() = this["aws_session_token"]

/**
 * A role that the user must automatically assume, giving it semi-temporary access to a specific set of resources
 */
public val AwsConfiguration.roleArn: String?
    get() = this["role_arn"]

/**
 * Specifies which profile must be used to automatically assume the role specified by role_arn
 */
public val AwsConfiguration.sourceProfile: String?
    get() = this["source_profile"]
