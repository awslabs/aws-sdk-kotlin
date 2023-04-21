/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.client.config.RetryMode
import aws.smithy.kotlin.runtime.config.*
import aws.smithy.kotlin.runtime.util.PlatformEnvironProvider

// NOTE: The JVM property names MUST match the ones defined in the Java SDK for any setting added.
// see: https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/SdkSystemSetting.java
// see: https://github.com/aws/aws-sdk-java-v2/blob/master/docs/LaunchChangelog.md#61-environment-variables-and-system-properties

/**
 * Settings to configure SDK runtime behavior
 */
@InternalSdkApi
public object AwsSdkSetting {
    /**
     * Configure the AWS access key ID.
     *
     * This value will not be ignored if the [AwsSecretAccessKey] is not specified.
     */
    public val AwsAccessKeyId: EnvironmentSetting<String> = strEnvSetting("AWS_ACCESS_KEY_ID", "aws.accessKeyId")

    /**
     * Configure the AWS secret access key.
     *
     * This value will not be ignored if the [AwsAccessKeyId] is not specified.
     */
    public val AwsSecretAccessKey: EnvironmentSetting<String> =
        strEnvSetting("AWS_SECRET_ACCESS_KEY", "aws.secretAccessKey")

    /**
     * Configure the AWS session token.
     */
    public val AwsSessionToken: EnvironmentSetting<String> = strEnvSetting("AWS_SESSION_TOKEN", "aws.sessionToken")

    /**
     * Configure the default region.
     */
    public val AwsRegion: EnvironmentSetting<String> = strEnvSetting("AWS_REGION", "aws.region")

    /**
     * Configure the default path to the shared config file.
     */
    public val AwsConfigFile: EnvironmentSetting<String> = strEnvSetting("AWS_CONFIG_FILE", "aws.configFile")

    /**
     * Configure the default path to the shared credentials profile file.
     */
    public val AwsSharedCredentialsFile: EnvironmentSetting<String> =
        strEnvSetting("AWS_SHARED_CREDENTIALS_FILE", "aws.sharedCredentialsFile")

    /**
     * The execution environment of the SDK user. This is automatically set in certain environments by the underlying AWS service.
     * For example, AWS Lambda will automatically specify a runtime indicating that the SDK is being used within Lambda.
     */
    public val AwsExecutionEnv: EnvironmentSetting<String> =
        strEnvSetting("AWS_EXECUTION_ENV", "aws.executionEnvironment")

    /**
     *  The name of the default profile that should be loaded from config
     */
    public val AwsProfile: EnvironmentSetting<String> = strEnvSetting("AWS_PROFILE", "aws.profile").orElse("default")

    /**
     * Whether to load information such as credentials, regions from EC2 Metadata instance service.
     */
    public val AwsEc2MetadataDisabled: EnvironmentSetting<Boolean> =
        boolEnvSetting("AWS_EC2_METADATA_DISABLED", "aws.disableEc2Metadata").orElse(false)

    /**
     * The EC2 instance metadata service endpoint.
     *
     * This allows a service running in EC2 to automatically load its credentials and region without needing to configure them
     * directly.
     */
    public val AwsEc2MetadataServiceEndpoint: EnvironmentSetting<String> =
        strEnvSetting("AWS_EC2_METADATA_SERVICE_ENDPOINT", "aws.ec2MetadataServiceEndpoint")

    /**
     * The endpoint mode to use when connecting to the EC2 metadata service endpoint
     */
    public val AwsEc2MetadataServiceEndpointMode: EnvironmentSetting<String> =
        strEnvSetting("AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE", "aws.ec2MetadataServiceEndpointMode")

    // TODO - Currently env/system properties around role ARN, role session name, etc are restricted to the STS web identity provider.
    //        They should be applied more broadly but this needs fleshed out across AWS SDKs before we can do so.

    /**
     * The ARN of a role to assume
     */
    public val AwsRoleArn: EnvironmentSetting<String> = strEnvSetting("AWS_ROLE_ARN", "aws.roleArn")

    /**
     * The session name to use for assumed roles
     */
    public val AwsRoleSessionName: EnvironmentSetting<String> =
        strEnvSetting("AWS_ROLE_SESSION_NAME", "aws.roleSessionName")

    /**
     * The AWS web identity token file path
     */
    public val AwsWebIdentityTokenFile: EnvironmentSetting<String> =
        strEnvSetting("AWS_WEB_IDENTITY_TOKEN_FILE", "aws.webIdentityTokenFile")

    /**
     * The elastic container metadata service path that should be called by the [aws.sdk.kotlin.runtime.auth.credentials.EcsCredentialsProvider]
     * when loading credentials from the container metadata service.
     */
    public val AwsContainerCredentialsRelativeUri: EnvironmentSetting<String> =
        strEnvSetting("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI", "aws.containerCredentialsPath")

    /**
     * The full URI path to a localhost metadata service to be used. This is ignored if
     * [AwsContainerCredentialsRelativeUri] is set.
     */
    public val AwsContainerCredentialsFullUri: EnvironmentSetting<String> =
        strEnvSetting("AWS_CONTAINER_CREDENTIALS_FULL_URI", "aws.containerCredentialsFullUri")

    /**
     * An authorization token to pass to a container metadata service.
     */
    public val AwsContainerAuthorizationToken: EnvironmentSetting<String> =
        strEnvSetting("AWS_CONTAINER_AUTHORIZATION_TOKEN", "aws.containerAuthorizationToken")

    /**
     * The maximum number of request attempts to perform. This is one more than the number of retries, so
     * aws.maxAttempts = 1 will have 0 retries.
     */
    public val AwsMaxAttempts: EnvironmentSetting<Int> = intEnvSetting("AWS_MAX_ATTEMPTS", "aws.maxAttempts")

    /**
     * Which RetryMode to use for the default RetryPolicy, when one is not specified at the client level.
     */
    public val AwsRetryMode: EnvironmentSetting<RetryMode> = enumEnvSetting("AWS_RETRY_MODE", "aws.retryMode")

    /**
     * Whether to use FIPS endpoints when making requests.
     */
    public val AwsUseFipsEndpoint: EnvironmentSetting<Boolean> =
        boolEnvSetting("AWS_USE_FIPS_ENDPOINT", "aws.useFipsEndpoint")

    /**
     * Whether to use dual-stack endpoints when making requests.
     */
    public val AwsUseDualStackEndpoint: EnvironmentSetting<Boolean> =
        boolEnvSetting("AWS_USE_DUALSTACK_ENDPOINT", "aws.useDualstackEndpoint")
}
