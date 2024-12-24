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
service TestService {
    version: "2023-01-01",
    operations: [HttpChecksumOperation, HttpChecksumRequestChecksumsNotRequiredOperation]
}

@http(uri: "/HttpChecksumOperation", method: "POST")
@optionalAuth
@httpChecksum(
    requestChecksumRequired: true,
    requestAlgorithmMember: "checksumAlgorithm",
    requestValidationModeMember: "validationMode",
    responseAlgorithms: ["CRC32", "CRC32C", "CRC64NVME", "SHA1", "SHA256"]
)
operation HttpChecksumOperation {
    input: SomeInput,
    output: SomeOutput
}

@http(uri: "/HttpChecksumRequestChecksumsNotRequiredOperation", method: "POST")
@optionalAuth
@httpChecksum(
    requestChecksumRequired: false,
    requestAlgorithmMember: "checksumAlgorithm",
    requestValidationModeMember: "validationMode",
    responseAlgorithms: ["CRC32", "CRC32C", "CRC64NVME", "SHA1", "SHA256"]
)
operation HttpChecksumRequestChecksumsNotRequiredOperation {
    input: SomeOtherInput,
    output: SomeOtherOutput
}

@input
structure SomeInput {
    @httpHeader("x-amz-request-algorithm")
    checksumAlgorithm: ChecksumAlgorithm

    @httpHeader("x-amz-response-validation-mode")
    validationMode: ValidationMode

    @httpHeader("x-amz-checksum-crc32")
    ChecksumCRC32: String

    @httpHeader("x-amz-checksum-crc32c")
    ChecksumCRC32C: String

    @httpHeader("x-amz-checksum-crc64nvme")
    ChecksumCRC64Nvme: String

    @httpHeader("x-amz-checksum-sha1")
    ChecksumSHA1: String

    @httpHeader("x-amz-checksum-sha256")
    ChecksumSHA256: String

    @httpHeader("x-amz-checksum-foo")
    ChecksumFoo: String

    @httpPayload
    @required
    body: Blob
}

@input
structure SomeOtherInput {
    @httpHeader("x-amz-request-algorithm")
    checksumAlgorithm: ChecksumAlgorithm

    @httpHeader("x-amz-response-validation-mode")
    validationMode: ValidationMode

    @httpHeader("x-amz-checksum-crc32")
    ChecksumCRC32: String

    @httpHeader("x-amz-checksum-crc32c")
    ChecksumCRC32C: String

    @httpHeader("x-amz-checksum-crc64nvme")
    ChecksumCRC64Nvme: String

    @httpHeader("x-amz-checksum-sha1")
    ChecksumSHA1: String

    @httpHeader("x-amz-checksum-sha256")
    ChecksumSHA256: String

    @httpHeader("x-amz-checksum-foo")
    ChecksumFoo: String

    @httpPayload
    @required
    body: Blob
}

@output
structure SomeOutput {}

@output
structure SomeOtherOutput {}

enum ChecksumAlgorithm {
    CRC32
    CRC32C
    CRC64NVME
    SHA1
    SHA256
}

enum ValidationMode {
    ENABLED
}