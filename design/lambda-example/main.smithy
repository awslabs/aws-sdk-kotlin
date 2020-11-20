$version: "1.0"
namespace com.amazonaws

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use aws.protocols#awsJson1_1


@awsJson1_1
service Lambda {
    version: "2015-03-31",
    operations: [Invoke, CreateAlias]
}


@http(method: "POST", uri: "/2015-03-31/functions/{FunctionName}/invocations")
operation Invoke{
    input: InvocationRequest,
    output: InvocationResponse,
    errors: [
        // real lambda model has quite a few more errors
        EC2AccessDeniedException,
        KMSAccessDeniedException,
        ResourceNotFoundException,
        TooManyRequestsException
    ]
}


@length(min: 1, max: 140)
@pattern("(arn:(aws[a-zA-Z-]*)?:lambda:)?([a-z]{2}(-gov)?-[a-z]+-\\d{1}:)?(\\d{12}:)?(function:)?([a-zA-Z0-9-_]+)(:(\\$LATEST|[a-zA-Z0-9-_]+))?")
string FunctionName


@enum([
    {
        value: "Event",
        name: "Event"
    },
    {
        value: "RequestResponse",
        name: "RequestResponse"
    },
    {
        value: "DryRun",
        name: "DryRun"
    }
])
string InvocationType


@enum([
    {
        value: "None",
        name: "None"
    },
    {
        value: "Tail",
        name: "Tail"
    }
])
string LogType

@length(min: 1, max: 128)
@pattern("(|[a-zA-Z0-9$_-]+)")
string Qualifier

structure InvocationRequest{
    @httpHeader("X-Amz-Client-Context")
    ClientContext: String

    @httpLabel
    @required
    FunctionName: FunctionName


    @httpHeader("X-Amz-Invocation-Type")
    InvocationType: InvocationType


    @httpHeader("X-Amz-Log-Type")
    LogType: LogType

    @httpQuery
    Qualifier: Qualifier 

    @httpPayload
    Payload: Blob

}


@length(min: 1, max: 1024)
@pattern("(\\$LATEST|[0-9]+)")
string Version


structure InvocationResponse {

    @httpHeader("X-Amz-Executed-Version")
    ExecutedVersion: Version
    
    @httpHeader("X-Amz-Function-Error")
    FunctionError: String

    @httpHeader("X-Amz-Log-Result")
    LogResult: String

    @httpPayload
    Payload: Blob

}



@error("server")
@httpError(502)
structure EC2AccessDeniedException {
    Message: String
    Type: String
}

@error("server")
@httpError(502)
structure KMSAccessDeniedException {
    Message: String
    Type: String
}

@error("client")
@httpError(404)
structure ResourceNotFoundException {
    Message: String
    Type: String
}

@error("client")
@httpError(400)
structure InvalidParameterValueException{
    Message: String
    Type: String
}

@enum([
    {
        value: "ConcurrentInvocationLimitExceeded",
        name: "ConcurrentInvocationLimitExceeded"
    },
    {
        value: "FunctionInvocationRateLimitExceeded",
        name: "FunctionInvocationRateLimitExceeded"
    },
    {
        value: "ReservedFunctionConcurrentInvocationLimitExceeded",
        name: "ReservedFunctionConcurrentInvocationLimitExceeded"
    },
    {
        value: "ReservedFunctionInvocationRateLimitExceeded",
        name: "ReservedFunctionInvocationRateLimitExceeded"
    },
    {
        value: "CallerRateLimitExceeded",
        name: "CallerRateLimitExceeded"
    }
])
string ThrottleReason


@error("client")
@httpError(429)
structure TooManyRequestsException {
    Reason: ThrottleReason
    @httpHeader("Retry-After")
    retryAfterSeconds: String
    message: String
    Type: String
}



@http(method: "POST", uri: "/2015-03-31/functions/{FunctionName}/aliases", code: 201)
operation CreateAlias{
    input: CreateAliasRequest,
    // FIXME - Model actually has the response as just "AliasConfiguration", we have to decide if we want to return
    // types directly or always wrap them in a "Response" the way e.g. v2 Java SDK does (CreateAliasResponse)
    output: AliasConfiguration,
    errors: [
        // real lambda model has more errors
        InvalidParameterValueException,
        ResourceNotFoundException,
        TooManyRequestsException
    ]
}

@length(min: 0, max: 256)
string Description

@length(min: 1, max: 128)
@pattern("(?!^[0-9]+$)([a-zA-Z0-9-_]+)")
string Alias

@length(min: 1, max: 1024)
@pattern("[0-9]+"
string AdditionalVersion
map<AdditionalVersion, Weight> AdditionalVersion

structure AliasRoutingConfiguration {
    AdditionalVersionWeights: AdditionalVersionWeights
}


structure CreateAliasRequest {
    Description: Description

    @httpLabel
    @required
    FunctionName: FunctionName

    @required
    FunctionVersion: Version

    @required
    Name: Alias

    RoutingConfig: AliasRoutingConfiguration
}

@pattern("arn:(aws[a-zA-Z-]*)?:lambda:[a-z]{2}(-gov)?-[a-z]+-\\d{1}:\\d{12}:function:[a-zA-Z0-9-_]+(:(\\$LATEST|[a-zA-Z0-9-_]+))?")
string FunctionArn

structure AliasConfiguration {
    AliasArn: FunctionArn
    Description: Description
    FunctionVersion: Version
    Name: Alias
    RevisionId: String
    RoutingConfig: AliasRoutingConfiguration
}
