//package aws.sdk.kotlin.e2etest
//
//import aws.sdk.kotlin.e2etest.S3TestUtils.createMultiRegionAccessPoint
//import aws.sdk.kotlin.e2etest.S3TestUtils.deleteBucketAndAllContents
//import aws.sdk.kotlin.e2etest.S3TestUtils.deleteMultiRegionAccessPoint
//import aws.sdk.kotlin.e2etest.S3TestUtils.getAccountId
//import aws.sdk.kotlin.e2etest.S3TestUtils.getBucketWithPrefix
//import aws.sdk.kotlin.e2etest.S3TestUtils.getMultiRegionAccessPointArn
//import aws.sdk.kotlin.e2etest.S3TestUtils.multiRegionAccessPointWasCreated
//import aws.sdk.kotlin.services.s3.S3Client
//import aws.sdk.kotlin.services.s3.deleteObject
//import aws.sdk.kotlin.services.s3.putObject
//import aws.sdk.kotlin.services.s3.withConfig
//import kotlinx.coroutines.runBlocking
//import org.junit.jupiter.api.AfterAll
//import org.junit.jupiter.api.BeforeAll
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.TestInstance
//
//// TODO: Test delete objects, get object, upload part.
//// TODO: Currently a lot of tests are failing. Is it because of my code changes or because of only a certain bucket being allowlisted.
//// TODO: Will I have to get rid of the weird streaming behavior I'm seeing ?
//
//// TODO: Errors I'm seeing are: HTTP body type is not supported - Request signature does not match the signature you provided - Checksum type mismatch (expected null but was crc32) - Missing header "transfer-encoding"
//// TODO: Use allow listed bucket
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class S3ChecksumTest {
//    private val s3West = S3Client { region = "us-west-2" }
//    private val objectKey = "test.txt"
//    private lateinit var accountId: String
//    private lateinit var usWestBucket: String
//
//    @BeforeAll
//    private fun setUp(): Unit = runBlocking {
//        accountId = getAccountId()
//        usWestBucket = getBucketWithPrefix(s3West, MRAP_BUCKET_PREFIX, "us-west-2", accountId)
//    }
//
//    @AfterAll
//    private fun cleanUp(): Unit = runBlocking {
//        deleteBucketAndAllContents(s3West, usWestBucket)
//        s3West.close()
//    }
//
//    @Test
//    fun testMultiRegionAccessPointOperation(): Unit = runBlocking {
//        s3SigV4a.putObject {
//            bucket = multiRegionAccessPointArn
//            key = objectKey
//        }
//
//        s3SigV4a.deleteObject {
//            bucket = multiRegionAccessPointArn
//            key = objectKey
//        }
//    }
//}