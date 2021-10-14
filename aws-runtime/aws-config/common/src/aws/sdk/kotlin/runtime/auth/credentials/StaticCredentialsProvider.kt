/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

/**
 * A credentials provider for a fixed set of credentials
 *
 * @param credentials The set of static credentials this provider will return
 */
public class StaticCredentialsProvider public constructor(private val credentials: Credentials) : CredentialsProvider {

    private constructor(builder: Builder) : this(Credentials(builder.accessKeyId!!, builder.secretAccessKey!!, builder.sessionToken))

    override suspend fun getCredentials(): Credentials = credentials

    public companion object {
        /**
         * Construct a new [StaticCredentialsProvider] using the given [block]
         */
        public operator fun invoke(block: Builder.() -> Unit): StaticCredentialsProvider = Builder().apply(block).build()
    }

    public class Builder {
        public var accessKeyId: String? = null
        public var secretAccessKey: String? = null
        public var sessionToken: String? = null

        public fun build(): StaticCredentialsProvider {
            if (accessKeyId == null || secretAccessKey == null) {
                throw IllegalArgumentException("StaticCredentialsProvider - accessKeyId and secretAccessKey must not be null")
            }
            return StaticCredentialsProvider(this)
        }
    }
}
