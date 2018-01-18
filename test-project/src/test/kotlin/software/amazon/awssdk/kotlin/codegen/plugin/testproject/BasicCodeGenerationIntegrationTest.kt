package software.amazon.awssdk.kotlin.codegen.plugin.testproject

import com.natpryce.hamkrest.anyElement
import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import org.apache.commons.logging.LogFactory
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import software.amazon.awssdk.core.regions.Region
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.StreamingResponseHandler
import software.amazon.awssdk.kotlin.services.s3.S3Client
import software.amazon.awssdk.kotlin.services.s3.model.*
import software.amazon.awssdk.kotlin.services.s3.model.BucketLocationConstraint.US_WEST_1
import software.amazon.awssdk.kotlin.services.ses.model.*
import software.amazon.awssdk.kotlin.services.ses.model.Destination
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import java.io.File
import java.time.Instant
import software.amazon.awssdk.services.s3.S3Client as JavaS3Client


class BasicCodeGenerationIntegrationTest {
    @Test
    fun vanillaServiceCallUsingKotlinWrapper() {
        assert.that(s3.listBuckets(ListBucketsRequest()).buckets, present(anyElement(has(Bucket::name, equalTo(bucketName)))))
    }

    @Test
    fun heavilyNestedRequest() {
        val standardSyntax = SendEmailRequest(
                destination = Destination(
                        toAddresses = listOf("someone@example.com")
                ),
                replyToAddresses = listOf("someone_else@example.com"),
                message = Message(
                        subject = Content(
                                data = "The Email Subject"
                        ),
                        body = Body(
                                text = Content(
                                        data = "The email body",
                                        charset = "UTF-8"
                                )
                        )
                )
        )

        val prettySyntax = SendEmailRequest {
            destination {
                toAddresses = listOf("someone@example.com")
            }
            replyToAddresses = listOf("someone_else@example.com")
            message {
                subject {
                    data = "The Email Subject"
                }
                body {
                    text {
                        data = "The email body"
                        charset = "UTF-8"
                    }
                }
            }
        }

        assert.that(standardSyntax, equalTo(prettySyntax))
    }

    @Test
    fun canUseStreamingOperations() {
        val testFile = File(javaClass.getResource("/test.file").toURI())
        val key = "object.txt"
        s3.putObject(PutObjectRequest(bucket = bucketName, key = key), RequestBody.of(testFile))

        val contents = s3.getObject(GetObjectRequest(bucket = bucketName, key = key), StreamingResponseHandler<GetObjectResponse, String> { _, inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        })

        assert.that(contents, equalTo(testFile.readText()))
    }

    companion object {
        private val bucketName = "aws-kotlin-sdk-test-" + Instant.now().toEpochMilli()
        private val s3 = S3Client { region(Region.US_WEST_1) }
        private val log = LogFactory.getLog(BasicCodeGenerationIntegrationTest::class.java)

        @BeforeClass
        @JvmStatic
        fun createBucket() {
            s3.createBucket {
                bucket = bucketName
                createBucketConfiguration {
                    locationConstraint = US_WEST_1
                }
            }
        }

        @AfterClass
        @JvmStatic
        fun cleanUp() {
            try {
                s3.listObjects {
                    bucket = bucketName
                }.contents?.forEach {
                    s3.deleteObject {
                        bucket = bucketName
                        key = it.key
                    }
                }
            } catch (e: Exception) {
                log.error("Problem deleting objects from bucket", e)
            }

            try {
                s3.deleteBucket {
                    bucket = bucketName
                }
            } catch (e: NoSuchBucketException) {
                log.error("Problem deleting bucket", e)
            }
        }
    }
}