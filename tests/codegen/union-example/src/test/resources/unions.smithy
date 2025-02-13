$version: "2"
namespace aws.sdk.kotlin.test

use aws.protocols#awsJson1_0
use smithy.rules#operationContextParams
use smithy.rules#endpointRuleSet
use aws.api#service

@awsJson1_0
@service(sdkId: "UnionOperationTest")
service TestService {
    operations: [DeleteObjects],
    version: "1"
}

operation DeleteObjects {
    input: DeleteObjectsRequest
}

structure DeleteObjectsRequest {
    Delete: Foo
}

union Foo {
    foo: Boolean
}
