package aws.sdk.kotlin.s3.transfermanager.data

data class S3Uri(val bucket: String, val key: String) {

    // this constructor receives a s3URI with format of S3://bucket-name/key-name
    // try to split bucket and key from it and store them separately
    constructor(s3Path: String): this(
                                        s3Path.substringAfter("S3://").substringBefore('/'),
                                        s3Path.substringAfter("S3://").substringAfter('/')
                                     ) {}
}
