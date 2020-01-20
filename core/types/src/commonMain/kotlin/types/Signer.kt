package types

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
    /**
     * A set of strings whose members represents headers that cannot be signed.
     * All headers in the provided request will have their names converted to
     * lower case and then checked for existence in the unsignableHeaders set.
     */
    val unsignableHeaders: Set<String>?,
    /**
     * A set of strings whose members represents headers that should be signed.
     * Any values passed here will override those provided via unsignableHeaders,
     * allowing them to be signed.
     *
     * All headers in the provided request will have their names converted to
     * lower case before signing.
     */
    val signableHeaders: Set<String>?,

    signingDate: DateTime = DateTime.now()
) : SigningArguments(signingDate)

interface Signer {
    /**
     * Method that takes in an request and returns a signed version of the request.
     *
     * @param request The request to sign
     * @param signingOptions Contains the attributes required for signing the request
     * @return A signed version of the input request
     */
    fun sign(request: HttpRequest, signingOptions: RequestSigningArguments?): HttpRequest
}