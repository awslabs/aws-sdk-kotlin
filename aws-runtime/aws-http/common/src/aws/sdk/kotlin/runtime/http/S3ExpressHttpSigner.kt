package aws.sdk.kotlin.runtime.http

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningAttributes
import aws.smithy.kotlin.runtime.collections.toMutableAttributes
import aws.smithy.kotlin.runtime.http.auth.AwsHttpSigner
import aws.smithy.kotlin.runtime.http.auth.HttpSigner
import aws.smithy.kotlin.runtime.http.auth.SignHttpRequest
import aws.smithy.kotlin.runtime.http.request.header

private const val S3_EXPRESS_SESSION_TOKEN_HEADER = "X-Amz-S3Session-Token"
private const val SESSION_TOKEN_HEADER = "X-Amz-Security-Token"

public class S3ExpressHttpSigner(
    public val awsHttpSigner: AwsHttpSigner
): HttpSigner {
    public override suspend fun sign(signingRequest: SignHttpRequest) {
        val sessionToken = (signingRequest.identity as? Credentials)?.sessionToken
            ?: error("S3ExpressHttpSigner failed to parse sessionToken from identity")

        // 1. add the S3 Express Session Token header
        signingRequest.httpRequest.header(S3_EXPRESS_SESSION_TOKEN_HEADER, sessionToken)
        println("Adding session token $sessionToken")

        // 2. enable omitSessionToken for awsHttpSigner
        val mutAttrs = signingRequest.signingAttributes.toMutableAttributes()
        mutAttrs[AwsSigningAttributes.OmitSessionToken] = true

        // 3. call main signer
        awsHttpSigner.sign(signingRequest.copy(signingAttributes = mutAttrs))

        // 4. remove standard session token header
        signingRequest.httpRequest.headers.remove(SESSION_TOKEN_HEADER)
    }
}