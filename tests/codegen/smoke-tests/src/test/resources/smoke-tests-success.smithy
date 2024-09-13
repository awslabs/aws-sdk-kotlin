$version: "2"
namespace com.test

use aws.protocols#awsJson1_0
use aws.api#service
use smithy.test#smokeTests
use smithy.rules#endpointRuleSet

@trait(selector: "service")
structure successResponseTrait { }

@successResponseTrait
@awsJson1_0
@service(sdkId: "Success")
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
service SuccessService {
    version: "1.0.0",
    operations: [ TestOperation ],
}

@smokeTests(
    [
        {
            id: "SuccessTest"
            params: {bar: "2"}
            expect: {
                success: {}
            }
            vendorParamsShape: AwsVendorParams,
            vendorParams: {
                region: "eu-central-1"
            }
        },
        {
            id: "SuccessTestWithTags"
            params: {bar: "2"}
            tags: [
                "success"
            ]
            expect: {
                success: {}
            }
            vendorParamsShape: AwsVendorParams,
            vendorParams: {
                region: "eu-central-1"
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

structure AwsVendorParams {
    region: String
}