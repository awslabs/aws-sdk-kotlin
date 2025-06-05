# Module s3

## Binary Data

Binary data (streams) are represented as a [`ByteStream`][aws.smithy.kotlin.runtime.content.ByteStream]. To supply a
`ByteStream` there are several convenience functions including:

```kt
val req = PutObjectRequest {
    ...
    body = ByteStream.fromFile(file)
    // body = ByteStream.fromBytes(byteArray)
    // body = ByteStream.fromString("string")
}
```

Consuming a `ByteStream` similarly has easy ways to consume the stream:

```kt
s3.getObject(req) { resp -> {
    // resp.body is a ByteStream instance
    resp.body?.writeToFile(path)
    
    // NOTE: both of these will consume the stream and buffer it entirely in-memory!
    // resp.body?.toByteArray()
    // resp.body?.decodeToString()
}
```

See [`GetObjectResponse`][aws.sdk.kotlin.services.s3.model.GetObjectResponse] for more details.

## Streaming Responses

Streaming responses are scoped to a `block`. Instead of returning the response directly, you must pass a lambda which is
given access to the response (and the underlying stream). The result of the call is whatever the lambda returns.

```kt
val s3 = S3Client { ... }

val req = GetObjectRequest { ... }

val path = Paths.get("/tmp/download.txt")

val contentSize = s3.getObject(req) { resp ->
    // resp is valid until the end of the block
    // do not attempt to store or process the stream after the block returns
    
    val rc = resp.body?.writeToFile(path)
    rc
}
println("wrote $contentSize bytes to $path")
```

This scoped response simplifies lifetime management for both the caller and the runtime.

See [`getObject`][aws.sdk.kotlin.services.s3.S3Client.getObject] for more details.
