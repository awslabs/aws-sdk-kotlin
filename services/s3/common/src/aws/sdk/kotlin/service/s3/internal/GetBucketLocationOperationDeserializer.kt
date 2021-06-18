package aws.sdk.kotlin.service.s3.internal

import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.GetBucketLocationResponse
import software.aws.clientrt.client.ExecutionContext
import software.aws.clientrt.http.operation.HttpDeserialize
import software.aws.clientrt.http.readAll
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.serde.xml.dom.parseDom


internal class GetBucketLocationOperationDeserializer: HttpDeserialize<GetBucketLocationResponse> {

    override suspend fun deserialize(context: ExecutionContext, response: HttpResponse): GetBucketLocationResponse {
        val builder = GetBucketLocationResponse.builder()

        val payload = response.body.readAll()
        if (payload != null) {
            deserializeGetBucketLocationOperationBody(builder, payload)
        }
        return builder.build()
    }
}

private suspend fun deserializeGetBucketLocationOperationBody(builder: GetBucketLocationResponse.DslBuilder, payload: ByteArray) {
    val dom = parseDom(software.aws.clientrt.serde.xml.xmlStreamReader(payload))
    check(dom.name.local == "LocationConstraint") { "Expected top-level tag of 'LocationConstraint' but found ${dom.name}." }
    checkNotNull(dom.text) { "Did not receive a value for 'LocationConstraint' in response." }

    builder.locationConstraint = BucketLocationConstraint.fromValue(dom.text!!)
}
