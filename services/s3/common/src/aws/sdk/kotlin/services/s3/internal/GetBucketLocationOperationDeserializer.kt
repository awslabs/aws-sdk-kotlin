package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.GetBucketLocationResponse
import aws.smithy.kotlin.runtime.client.ExecutionContext
import aws.smithy.kotlin.runtime.http.operation.HttpDeserialize
import aws.smithy.kotlin.runtime.http.readAll
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.serde.xml.dom.parseDom
import aws.smithy.kotlin.runtime.serde.xml.xmlStreamReader

/**
 * Custom deserializer for the GetBucketLocation operation.  This operation does not conform to the model.
 * In the model, there is a nested tag of the same name as the top-level tag, however in practice
 * this child tag is not passed from the service.  In this implementation the model is not used for
 * deserialization.
 */
internal class GetBucketLocationOperationDeserializer : HttpDeserialize<GetBucketLocationResponse> {

    override suspend fun deserialize(context: ExecutionContext, response: HttpResponse): GetBucketLocationResponse {
        val builder = GetBucketLocationResponse.Builder()

        val payload = response.body.readAll()
        if (payload != null) {
            deserializeGetBucketLocationOperationBody(builder, payload)
        }
        return builder.build()
    }
}

private fun deserializeGetBucketLocationOperationBody(builder: GetBucketLocationResponse.Builder, payload: ByteArray) {
    val dom = parseDom(xmlStreamReader(payload))
    check(dom.name.local == "LocationConstraint") { "Expected top-level tag of 'LocationConstraint' but found ${dom.name}." }
    val rawLocationConstraint = checkNotNull(dom.text) { "Did not receive a value for 'LocationConstraint' in response." }

    builder.locationConstraint = BucketLocationConstraint.fromValue(rawLocationConstraint)
}
