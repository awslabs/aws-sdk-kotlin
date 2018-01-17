package software.amazon.awssdk.kotlin.codegen.plugin.testproject

import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.present
import org.junit.After
import org.junit.Test
import software.amazon.awssdk.core.regions.Region
import software.amazon.awssdk.kotlin.services.s3.S3Client
import software.amazon.awssdk.kotlin.services.s3.model.BucketLocationConstraint.US_WEST_1
import software.amazon.awssdk.kotlin.services.s3.model.ListBucketsRequest
import java.time.Instant
import software.amazon.awssdk.services.s3.S3Client as JavaS3Client


class BasicCodeGenerationIntegrationTest {
    private val bucketName = "aws-kotlin-sdk-test-" + Instant.now().toEpochMilli()
    private val client = S3Client { region(Region.US_WEST_1) }

    @Test
    fun canCallAServiceViaKotlin() {
        assert.that(client.listBuckets(ListBucketsRequest()).buckets, present())
    }

    @Test
    fun canCallViaApplySyntax() {
        val response = client.createBucket {
            bucket = bucketName
            createBucketConfiguration {
                locationConstraint = US_WEST_1
            }
        }

        assert.that(response.location, present())
    }

    @After
    fun cleanUp() {
        client.deleteBucket {
            bucket = bucketName
        }
    }
}