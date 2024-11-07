namespace aws.sdk.kotlin.test

use aws.protocols#{protocol-name}
use aws.api#service
use aws.auth#sigv4

@{protocol-name}
@sigv4(name: "event-stream-test")
@service(sdkId: "EventStreamTest")
service TestService { version: "123", operations: [TestStreamOp] }

{op-traits}
operation TestStreamOp {
    input: TestStreamInputOutput,
    output: TestStreamInputOutput,
    errors: [SomeError],
}

structure TestStreamInputOutput {
    @httpPayload
    @required
    value: TestStream
}

@error("client")
structure SomeError {
    Message: String,
}

union TestUnion {
    Foo: String,
    Bar: Integer,
}

structure TestStruct {
    someString: String,
    someInt: Integer,
}

structure MessageWithBlob { @eventPayload data: Blob }

structure MessageWithString { @eventPayload data: String }

structure MessageWithStruct { @eventPayload someStruct: TestStruct }

structure MessageWithUnion { @eventPayload someUnion: TestUnion }

structure MessageWithHeaders {
    @eventHeader blob: Blob,
    @eventHeader boolean: Boolean,
    @eventHeader byte: Byte,
    @eventHeader int: Integer,
    @eventHeader long: Long,
    @eventHeader short: Short,
    @eventHeader string: String,
    @eventHeader timestamp: Timestamp,
}
structure MessageWithHeaderAndPayload {
    @eventHeader header: String,
    @eventPayload payload: Blob,
}
structure MessageWithNoHeaderPayloadTraits {
    someInt: Integer,
    someString: String,
}

structure MessageWithUnboundPayloadTraits {
    @eventHeader header: String,
    unboundString: String,
}

@streaming
union TestStream {
    MessageWithBlob: MessageWithBlob,
    MessageWithString: MessageWithString,
    MessageWithStruct: MessageWithStruct,
    MessageWithUnion: MessageWithUnion,
    MessageWithHeaders: MessageWithHeaders,
    MessageWithHeaderAndPayload: MessageWithHeaderAndPayload,
    MessageWithNoHeaderPayloadTraits: MessageWithNoHeaderPayloadTraits,
    MessageWithUnboundPayloadTraits: MessageWithUnboundPayloadTraits,
    SomeError: SomeError,
}
