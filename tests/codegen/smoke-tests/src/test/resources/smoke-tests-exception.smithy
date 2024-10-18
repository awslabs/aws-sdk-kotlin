$version: "2"
namespace smithy.kotlin.traits

use aws.protocols#awsJson1_0
use aws.api#service
use smithy.test#smokeTests
use smithy.rules#endpointRuleSet

@trait(selector: "service")
structure failedResponseTrait { }

@failedResponseTrait
@awsJson1_0
@service(sdkId: "Exception")
@endpointRuleSet(
    version: "1.0",
    parameters: {},
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
service ExceptionService {
    version: "1.0.0",
    operations: [ TestOperation ],
}

@smokeTests(
    [
        {
            id: "ExceptionTest"
            expect: {
                success: {}
            }
        }
    ]
)
operation TestOperation {
    input := {
        bar: String
    }
    errors: [
        InvalidMessageError
    ]
}

@error("client")
structure InvalidMessageError {}
