$version: "2"
namespace aws.sdk.kotlin.test

use aws.api#service
use aws.auth#sigv4
use aws.protocols#httpChecksum
use aws.protocols#restJson1
use smithy.rules#endpointRuleSet

@service(sdkId: "dontcare")
@restJson1
@sigv4(name: "dontcare")
@auth([sigv4])
@endpointRuleSet({
    "version": "1.0",
    "rules": [{ "type": "endpoint", "conditions": [], "endpoint": { "url": "https://example.com" } }],
    "parameters": {
        "Region": { "required": false, "type": "String", "builtIn": "AWS::Region" },
    }
})
service ClientConfigTestService {
    version: "2023-01-01",
    operations: [ChecksumsNotRequiredOperation, ChecksumsRequiredOperation]
}

@httpChecksum(
    requestChecksumRequired: false,
    requestAlgorithmMember: "checksumAlgorithm",
)
@http(method: "POST", uri: "/test-checksums", code: 200)
operation ChecksumsNotRequiredOperation {
    input: SomeInput,
    output: SomeOutput
}

@httpChecksum(
    requestChecksumRequired: true,
    requestAlgorithmMember: "checksumAlgorithm",
)
@http(method: "POST", uri: "/test-checksums-2", code: 200)
operation ChecksumsRequiredOperation {
    input: AnotherInput,
    output: AnotherOutput
}

@input
structure SomeInput {
    @httpHeader("x-amz-request-algorithm")
    checksumAlgorithm: ChecksumAlgorithm

    @httpPayload
    @required
    body: String
}

@output
structure SomeOutput {}

@input
structure AnotherInput {
    @httpHeader("x-amz-request-algorithm")
    checksumAlgorithm: ChecksumAlgorithm

    @httpPayload
    @required
    body: String
}

@output
structure AnotherOutput {}

enum ChecksumAlgorithm {
    CRC32
    CRC32C
    CRC64NVME
    SHA1
    SHA256
}