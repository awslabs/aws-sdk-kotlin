package software.aws.kotlinsdk

data class AwsCredentials(val name: String, val accessKeyId: String, val secretAccessKey: String)

typealias AwsCredentialsProvider = () -> AwsCredentials?

typealias AwsCredentialsProviderChain = List<AwsCredentialsProvider>