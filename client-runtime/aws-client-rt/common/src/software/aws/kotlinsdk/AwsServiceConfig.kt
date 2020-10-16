package software.aws.kotlinsdk

interface AwsServiceConfig {
    val region: String?
    val credentialProviderChain: AwsCredentialsProviderChain?
}