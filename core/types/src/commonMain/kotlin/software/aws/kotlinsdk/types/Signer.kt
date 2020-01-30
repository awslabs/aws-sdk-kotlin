package software.aws.kotlinsdk.types

import com.soywiz.klock.DateTime

open class SigningArguments(
    /**
     * The date and time to be used as signature metadata. This value should be
     * a Date object, a unix (epoch) timestamp, or a string that can be
     * understood by the JavaScript `Date` constructor. If not supplied, the
     * value returned by `new Date()` will be used.
     */
    val signingDate: DateTime = DateTime.now()
)

class RequestSigningArguments(
    signingDate: DateTime = DateTime.now()
) : SigningArguments(signingDate)

interface Signer {
    /**
     * Method that takes in an request and returns a signed version of the request.
     *
     * @param originalRequest The request to sign
     * @param signingOptions Contains the attributes required for signing the request
     * @return A signed version of the input request
     */
    fun sign(originalRequest: HttpRequest, signingOptions: RequestSigningArguments?): HttpRequest
}