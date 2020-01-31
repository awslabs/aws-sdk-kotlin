# AWS SDK for Kotlin

The **AWS SDK for Kotlin** is a pure Kotlin multi-platform SDK for AWS that targets JS and JVM runtimes.

Allows making calls to AWS services in a more native Kotlin fashion:

```kotlin
val client = S3Client { region(Region.US_WEST_1) }
client.createBucket {
    bucket = "some-bucket-name"
    createBucketConfiguration {
        locationConstraint = US_WEST_1
    }
}
```

Prettier statically typed DSL - useful for creating heavily hierarchical request objects:

```kotlin
val request = SendEmailRequest {
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
```

## License

This library is licensed under the Apache 2.0 License. 
