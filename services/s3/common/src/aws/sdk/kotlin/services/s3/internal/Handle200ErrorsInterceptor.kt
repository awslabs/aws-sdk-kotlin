package aws.sdk.kotlin.services.s3.internal

import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse

internal class Handle200ErrorsInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeCompletion(context: ResponseInterceptorContext<Any, Any, HttpRequest?, HttpResponse?>): Result<Any> {
        return super.modifyBeforeCompletion(context)
    }
}