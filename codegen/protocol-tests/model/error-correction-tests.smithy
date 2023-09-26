$version: "2.0"

namespace aws.protocoltests.errorcorrection

use aws.api#service
use aws.protocols#awsJson1_0
use aws.protocols#restXml
use smithy.test#httpResponseTests

@service(sdkId: "Error Correction Json")
@awsJson1_0
service RequiredValueJson {
    operations: [SayHello],
    version: "1"
}


@service(sdkId: "Error Correction Xml")
@restXml
service RequiredValueXml {
    operations: [SayHelloXml],
    version: "1"
}

@error("client")
structure Error {
    @required
    requestId: String

    @required
    message: String
}

@http(method: "POST", uri: "/")
operation SayHello { output: TestOutputDocument, errors: [Error] }

@http(method: "POST", uri: "/")
operation SayHelloXml { output: TestOutput, errors: [Error] }

structure TestOutputDocument with [TestStruct] { innerField: Nested, @required document: Document }
structure TestOutput with [TestStruct] { innerField: Nested }

@mixin
structure TestStruct {
    @required
    foo: String,

    @required
    byteValue: Byte,

    @required
    intValue: Integer,

    @required
    listValue: StringList,

    @required
    mapValue: ListMap,

    @required
    nestedListValue: NestedList

    @required
    nested: Nested

    @required
    blob: Blob

    @required
    enum: MyEnum

    @required
    union: MyUnion

    notRequired: String

    @required
    timestampValue: Timestamp
}

enum MyEnum {
    A,
    B,
    C
}

union MyUnion {
    A: Integer,
    B: String,
    C: Unit
}

structure Nested {
    @required
    a: String
}

list StringList {
    member: String
}

list NestedList {
    member: StringList
}

map ListMap {
    key: String,
    value: StringList
}

// NOTE: there is no way to model enum or union defaults in an `httpResponseTest` because the default is the generated
// "SdkUnknown" variant.
apply SayHello @httpResponseTests([
    {
        id: "error_recovery_json",
        protocol: awsJson1_0,
        params: {
            union: { A: 5 },
            enum: "A",
            foo: "",
            byteValue: 0,
            intValue: 0,
            blob: "",
            listValue: [],
            mapValue: {},
            nestedListValue: [],
            document: null,
            nested: { a: "" },
            timestampValue: 0
        },
        code: 200,
        body: "{\"union\": { \"A\": 5 }, \"enum\": \"A\" }"
    }
])

apply SayHelloXml @httpResponseTests([
    {
        id: "error_recovery_xml",
        protocol: restXml,
        params: {
            union: { A: 5 },
            enum: "A",
            foo: "",
            byteValue: 0,
            intValue: 0,
            blob: "",
            listValue: [],
            mapValue: {},
            nestedListValue: [],
            nested: { a: "" },
            timestampValue: 0
        },
        code: 200,
        body: "<TestOutput><union><A>5</A></union><enum>A</enum></TestOutput>"
    }
])