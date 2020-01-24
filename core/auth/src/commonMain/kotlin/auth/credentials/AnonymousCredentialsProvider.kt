//package auth.credentials
//
///**
// * Credentials provider that always returns anonymous [AwsCredentials]. Anonymous AWS credentials result in un-authenticated
// * requests and will fail unless the resource or API's policy has been configured to specifically allow anonymous access.
// */
//class AnonymousCredentialsProvider private constructor() : AwsCredentialsProvider {
//    override fun resolveCredentials(): AwsCredentials {
//        return CREDENTIALS
//    }
//
//    companion object {
//        private val CREDENTIALS by lazy { AnonymousCredentials() }
//
//        fun create(): AwsCredentialsProvider {
//            return AnonymousCredentialsProvider()
//        }
//    }
//}
//
//class AnonymousCredentials : AwsCredentials {
//    override val accessKeyId: String = throw UnsupportedOperationException()
//    override val secretAccessKey: String = throw UnsupportedOperationException()
//
//    override fun toString(): String {
//        return "AnonymousCredentials()"
//    }
//}