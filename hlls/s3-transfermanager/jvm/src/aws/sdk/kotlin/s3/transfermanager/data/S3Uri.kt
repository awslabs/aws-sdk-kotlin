package aws.sdk.kotlin.s3.transfermanager.data

data class S3Uri(val bucket: String, val key: String)

/**
 * this constructor receives a s3URI with format of S3://bucket-name/key-name
 * try to split bucket and key from it, validate format, and store them separately
 */
fun S3Uri(s3Path: String): S3Uri {
    if (!s3Path.startsWith("s3://")) {
        throw IllegalArgumentException("s3Path input is invalid: URIs must start with `s3://`")
    }
    val bucket = s3Path.substringAfter("s3://").substringBefore('/')
    val key = s3Path.substringAfter("s3://").substringAfter('/')

    // check bucket and key is not empty and has valid format
    require(bucket.isNotEmpty()) { "s3Path input is invalid: bucket name can not be empty" }
    require(key.isNotEmpty()) { "s3Path input is invalid: key name can not be empty" }

    return S3Uri(bucket, key)
}
