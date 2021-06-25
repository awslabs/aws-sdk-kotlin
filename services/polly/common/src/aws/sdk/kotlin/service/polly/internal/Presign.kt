package aws.sdk.kotlin.service.polly.internal

import aws.sdk.kotlin.runtime.auth.PresignedRequestConfig
import aws.sdk.kotlin.runtime.auth.PresignedRequest
import aws.sdk.kotlin.runtime.auth.presignUrl
import aws.sdk.kotlin.services.polly.model.SynthesizeSpeechRequest
import aws.smithy.kotlin.runtime.util.text.urlEncodeComponent

// This function adapts Polly specific state to general state suitable to sign the request generically (without service knowledge)
suspend fun SynthesizeSpeechRequest.presign(config: PresignedRequestConfig): PresignedRequest {
    val sb = StringBuilder()
    sb.append("${config.endpoint.protocol}://${config.endpoint.hostname}${config.path}?")
    sb.append("Text=${text!!.urlEncodeComponent()}&VoiceId=${voiceId!!.toString().urlEncodeComponent()}&OutputFormat=${outputFormat.toString()!!.urlEncodeComponent()}")
    val url = sb.toString()

    return presignUrl(config, url)
}
