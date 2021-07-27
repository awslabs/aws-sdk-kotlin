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

// FIXME - when we implement virtual host addressing as the default will need to change the host and uri
//         see: https://github.com/awslabs/aws-sdk-kotlin/issues/220
apply PutObject @httpRequestTests([
    {
        id: "PutObjectDefaultContentType",
        documentation: "This test case validates default content-type behavior when not specified in the request",
        protocol: "aws.protocols#restXml",
        method: "PUT",
        uri: "/mybucket/mykey",
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
        uri: "/mybucket/mykey",
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
