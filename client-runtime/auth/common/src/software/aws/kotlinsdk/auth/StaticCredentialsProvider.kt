/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.aws.kotlinsdk.auth

/**
 * A credentials provider for a fixed set of credentials
 */
public class StaticCredentialsProvider private constructor(private val credentials: Credentials) : CredentialsProvider {

    private constructor(builder: Builder) : this(Credentials(builder.accessKeyId!!, builder.secretAccessKey!!, builder.sessionToken))

    override suspend fun getCredentials(): Credentials {
        return credentials
    }

    public companion object {
        /**
         * Construct a new [StaticCredentialsProvider] using the given [block]
         */
        public fun build(block: Builder.() -> Unit): StaticCredentialsProvider = Builder().apply(block).build()

        /**
         * Construct a new [StaticCredentialsProvider] from a set of credentials
         */
        public fun fromCredentials(credentials: Credentials): StaticCredentialsProvider = StaticCredentialsProvider(credentials)
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
