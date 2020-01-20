package auth.signer.internal

import annotations.SdkInternalApi
import auth.credentials.AwsBasicCredentials
import auth.credentials.AwsCredentials
import auth.credentials.AwsSessionCredentials
import auth.signer.Sha256
import core.exception.SdkClientException
import core.io.SdkDigestInputStream
import core.signer.Signer
import http.ContentStreamProvider
import http.SdkHttpFullRequest
import okio.Source
import types.QueryParameters
import types.SdkException
import types.Signer
import utils.BinaryUtils
import utils.StringUtils
import utils.http.SdkHttpUtils
import java.io.ByteArrayInputStream
import java.lang.Exception
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.SortedMap
import java.util.TreeMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.jvm.Throws

/**
 * Abstract base class for AWS signing protocol implementations. Provides
 * utilities commonly needed by signing protocols such as computing
 * canonicalized host names, query string parameters, etc.
 */
abstract class AbstractAwsSigner : Signer {
    private val messageDigest = Sha256()

    /**
     * Hashes the string contents (assumed to be UTF-8) using the SHA-256 algorithm.
     *
     * @param text The string to hash.
     * @return The hashed bytes from the specified string.
     */
    private fun hash(text: String): ByteArray {
        messageDigest.update(text.encodeToByteArray())
        return messageDigest.digest()
    }

    /**
     * Computes an RFC 2104-compliant HMAC signature and returns the result as a
     * Base64 encoded string.
     */
    protected fun signAndBase64Encode(
        data: String?, key: String?,
        algorithm: SigningAlgorithm?
    ): String? {
        return signAndBase64Encode(data.toByteArray(java.nio.charset.StandardCharsets.UTF_8), key, algorithm)
    }

    /**
     * Computes an RFC 2104-compliant HMAC signature for an array of bytes and
     * returns the result as a Base64 encoded string.
     */
    @Throws(SdkClientException::class)
    private fun signAndBase64Encode(
        data: ByteArray?, key: String?,
        algorithm: SigningAlgorithm?
    ): String? {
        return try {
            val signature =
                sign(data, key.toByteArray(java.nio.charset.StandardCharsets.UTF_8), algorithm)
            BinaryUtils.toBase64(signature)
        } catch (e: Exception) {
            throw SdkClientException.builder()
                .message("Unable to calculate a request signature: " + e.message)
                .cause(e)
                .build()
        }
    }

    protected fun signWithMac(stringData: String?, mac: Mac?): ByteArray? {
        return try {
            mac.doFinal(stringData.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
        } catch (e: Exception) {
            throw SdkClientException.builder()
                .message("Unable to calculate a request signature: " + e.message)
                .cause(e)
                .build()
        }
    }

    @Throws(SdkClientException::class)
    protected fun sign(
        stringData: String?, key: ByteArray?,
        algorithm: SigningAlgorithm?
    ): ByteArray? {
        return try {
            val data: ByteArray = stringData.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
            sign(data, key, algorithm)
        } catch (e: Exception) {
            throw SdkClientException.builder()
                .message("Unable to calculate a request signature: " + e.message)
                .cause(e)
                .build()
        }
    }

    @Throws(SdkClientException::class)
    protected fun sign(
        data: ByteArray?,
        key: ByteArray?,
        algorithm: SigningAlgorithm?
    ): ByteArray? {
        return try {
            val mac: Mac = algorithm.getMac()
            mac.init(SecretKeySpec(key, algorithm.toString()))
            mac.doFinal(data)
        } catch (e: Exception) {
            throw SdkClientException.builder()
                .message("Unable to calculate a request signature: " + e.message)
                .cause(e)
                .build()
        }
    }

    @Throws(SdkClientException::class)
    fun hash(input: Source): ByteArray? {
        return try {
            val md: MessageDigest? = messageDigestInstance
            val digestInputStream: DigestInputStream = SdkDigestInputStream(
                input, md
            )
            val buffer = ByteArray(1024)
            while (digestInputStream.read(buffer) > -1) {
            }
            digestInputStream.getMessageDigest().digest()
        } catch (e: Exception) {
            throw SdkException("Unable to compute hash while signing request: ${e.message}", e)
        }
    }

    /**
     * Hashes the binary data using the SHA-256 algorithm.
     *
     * @param data The binary data to hash.
     * @return The hashed bytes from the specified data.
     * @throws SdkClientException If the hash cannot be computed.
     */
    fun hash(data: ByteArray?): ByteArray? {
        return try {
            val md: MessageDigest? = messageDigestInstance
            md.update(data)
            md.digest()
        } catch (e: Exception) {
            throw SdkException("Unable to compute hash while signing request: ${e.message}", e)
        }
    }

    /**
     * Examines the specified query string parameters and returns a canonicalized form.
     *
     * The canonicalized query string is formed by first sorting all the query
     * string parameters, then URI encoding both the key and value and then
     * joining them, in order, separating key value pairs with an '&amp;'.
     *
     * @param parameters The query string parameters to be canonicalized.
     * @return A canonicalized form for the specified query string parameters.
     */
    protected fun getCanonicalizedQueryString(parameters: QueryParameters): String {
        val sorted = parameters.entries.asSequence().sortedBy { it.key }
        /**
         * Signing protocol expects the param values also to be sorted after url
         * encoding in addition to sorted parameter names.
         */
        for (entry in parameters.entries) {
            val encodedParamName: String = SdkHttpUtils.urlEncode(entry.key)
            val paramValues = entry.value
            val encodedValues: MutableList<String?> = java.util.ArrayList(paramValues!!.size)
            for (value in paramValues) {
                val encodedValue: String = SdkHttpUtils.urlEncode(value)

                // Null values should be treated as empty for the purposes of signing, not missing.
                // For example "?foo=" instead of "?foo".
                val signatureFormattedEncodedValue = encodedValue ?: ""
                encodedValues.add(signatureFormattedEncodedValue)
            }
            java.util.Collections.sort(encodedValues)
            sorted.put(encodedParamName, encodedValues)
        }
        return SdkHttpUtils.flattenQueryParameters(sorted).orElse("")
    }

    protected fun getBinaryRequestPayloadStream(streamProvider: ContentStreamProvider?): java.io.InputStream? {
        return try {
            if (streamProvider == null) {
                ByteArrayInputStream(ByteArray(0))
            } else streamProvider.newStream()
        } catch (e: SdkException) {
            throw e
        } catch (e: Exception) {
            throw SdkException("Unable to read request payload to sign request: ${e.message}", e)
        }
    }

    fun getCanonicalizedResourcePath(resourcePath: String, urlEncode: Boolean): String? {
        return if (resourcePath.isEmpty()) {
            "/"
        } else {
            val value = if (urlEncode) SdkHttpUtils.urlEncodeIgnoreSlashes(resourcePath) else resourcePath
            if (value.startsWith('/')) {
                value
            } else {
                "/$value"
            }
        }
    }

    protected fun getCanonicalizedEndpoint(request: SdkHttpFullRequest?): String? {
        var endpointForStringToSign: String = StringUtils.lowerCase(request.host())

        // Omit the port from the endpoint if we're using the default port for the protocol. Some HTTP clients (ie. Apache) don't
        // allow you to specify it in the request, so we're standardizing around not including it. See SdkHttpRequest#port().
        if (!SdkHttpUtils.isUsingStandardPort(request.protocol(), request.port())) {
            endpointForStringToSign += ":" + request.port()
        }
        return endpointForStringToSign
    }

    /**
     * Loads the individual access key ID and secret key from the specified credentials, trimming any extra whitespace from the
     * credentials.
     *
     *
     * Returns either a [AwsSessionCredentials] or a [AwsBasicCredentials] object, depending on the input type.
     *
     * @return A new credentials object with the sanitized credentials.
     */
    protected fun sanitizeCredentials(credentials: AwsCredentials?): AwsCredentials? {
        val accessKeyId: String = StringUtils.trim(credentials!!.accessKeyId())
        val secretKey: String = StringUtils.trim(credentials!!.secretAccessKey())
        if (credentials is AwsSessionCredentials) {
            val sessionCredentials = credentials as AwsSessionCredentials?
            return AwsSessionCredentials.create(
                accessKeyId,
                secretKey,
                StringUtils.trim(sessionCredentials!!.sessionToken())
            )
        }
        return AwsBasicCredentials.create(accessKeyId, secretKey)
    }

    /**
     * Adds session credentials to the request given.
     *
     * @param mutableRequest The request to add session credentials information to
     * @param credentials    The session credentials to add to the request
     */
    protected abstract fun addSessionCredentials(
        mutableRequest: SdkHttpFullRequest.Builder?,
        credentials: AwsSessionCredentials?
    )
}