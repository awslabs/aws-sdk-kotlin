$version: "1.0"

namespace com.amazonaws.s3
use smithy.test#httpResponseTests
use smithy.test#httpRequestTests

apply NotFound @httpResponseTests([
    {
        id: "HeadObjectEmptyBody",
        documentation: "This test case validates https://github.com/awslabs/aws-sdk-kotlin/issues/152",
        params: {
        },
        body: "",
        protocol: "aws.protocols#restXml",
        code: 404,
        headers: {
            "x-amz-request-id": "GRZ6BZ468DF52F2E",
            "x-amz-id-2": "UTniwu6QmCIjVeuK2ZfeWBOnu7SqMQOS3Vac6B/K4H2ZCawYUl+nDbhGTImuyhZ5DFiojR3Kcz4=",
            "content-type": "application/xml",
            "date": "Thu, 03 Jun 2021 04:05:52 GMT",
            "server": "AmazonS3"
        }
    }
])

apply PutObject @httpRequestTests([
    {
        id: "PutObjectDefaultContentType",
        documentation: "This test case validates default content-type behavior when not specified in the request",
        protocol: "aws.protocols#restXml",
        method: "PUT",
        uri: "/mykey",
        host: "s3.us-west-2.amazonaws.com",
        body: "foobar",
        bodyMediaType: "application/octet-stream",
        headers: {
            "Content-Type": "application/octet-stream"
        },
        params: {
            Bucket: "mybucket",
            Key: "mykey",
            Body: "foobar"
        }
    },
    {
        id: "PutObjectExplicitContentType",
        documentation: "This test case validates https://github.com/awslabs/aws-sdk-kotlin/issues/193",
        protocol: "aws.protocols#restXml",
        method: "PUT",
        uri: "/mykey",
        host: "s3.us-west-2.amazonaws.com",
        body: "{\"foo\":\"bar\"}",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            Bucket: "mybucket",
            Key: "mykey",
            ContentType: "application/json",
            Body: "{\"foo\":\"bar\"}"
        }
    }
])

apply CreateBucket @httpRequestTests([
    {
        id: "CreateBucketNoBody",
        documentation: "Validates https://github.com/awslabs/aws-sdk-kotlin/issues/567 (empty body)",
        protocol: "aws.protocols#restXml",
        method: "PUT",
        uri: "/",
        host: "s3.us-west-2.amazonaws.com",
        body: "",
        forbidHeaders: ["Content-Type", "Content-Length"],
        params: {
            Bucket: "mybucket",
        }
    },
    {
        id: "CreateBucketWithBody",
        documentation: "This test case validates https://github.com/awslabs/aws-sdk-kotlin/issues/567 (non-empty body)",
        protocol: "aws.protocols#restXml",
        method: "PUT",
        uri: "/",
        host: "s3.us-west-2.amazonaws.com",
        body: "<CreateBucketConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><LocationConstraint>us-east-2</LocationConstraint></CreateBucketConfiguration>",
        headers: {
            "Content-Type": "application/xml",
            "Content-Length": "153"
        },
        params: {
            Bucket: "mybucket",
            CreateBucketConfiguration: {
                LocationConstraint: "us-east-2"
            }
        }
    }
])
