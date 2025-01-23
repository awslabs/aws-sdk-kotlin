$version: "2.0"
namespace aws.sdk.kotlin.test

use aws.protocols#awsJson1_0
use smithy.rules#operationContextParams
use smithy.rules#endpointRuleSet
use aws.api#service

@awsJson1_0
@service(sdkId: "OperationContextParamsTest")
@endpointRuleSet(
    version: "1.0",
    parameters: {
        "ObjectKeys": {
            "type": "stringArray",
            "documentation": "A string array.",
            "required": true
        }
    },
    rules: [
        {
            "type": "endpoint",
            "conditions": [],
            "endpoint": {
                "url": "https://static.endpoint"
            }
        }
    ]
)
service TestService {
    operations: [DeleteObjects],
    version: "1"
}

@operationContextParams(
    ObjectKeys: {
        path: "Delete.Objects[*].[Key][]"
    }
)
operation DeleteObjects {
    input: DeleteObjectsRequest
}

structure DeleteObjectsRequest {
    Delete: Delete
}

structure Delete {
    Objects: ObjectIdentifierList
}

list ObjectIdentifierList {
    member: ObjectIdentifier
}

structure ObjectIdentifier {
    Key: String
}
