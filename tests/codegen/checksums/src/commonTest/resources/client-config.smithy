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
    operations: [ChecksumsNotRequiredOperation]
}

@httpChecksum(
    requestChecksumRequired: false,
    requestAlgorithmMember: "checksumAlgorithm",
)
@http(method: "POST", uri: "/test-eventstream", code: 200)
operation ChecksumsNotRequiredOperation {
    input: SomeInput,
    output: SomeOutput
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

enum ChecksumAlgorithm {
    CRC32
    CRC32C
    CRC64NVME
    SHA1
    SHA256
}