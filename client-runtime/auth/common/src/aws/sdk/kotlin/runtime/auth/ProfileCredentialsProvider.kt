/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.crt.auth.credentials.build
import aws.sdk.kotlin.crt.io.ClientBootstrap
import aws.sdk.kotlin.crt.io.TlsContext
import aws.sdk.kotlin.crt.auth.credentials.ProfileCredentialsProvider as ProfileCredentialsProviderCrt

class ProfileCredentialsProvider private constructor(builder: Builder) : CrtBasedCredentialsProvider {
    override val crtProvider = ProfileCredentialsProviderCrt.build {
        clientBootstrap = builder.clientBootstrap
        tlsContext = builder.tlsContext
        profileName = builder.profileName
        configFileNameOverride = builder.configFileNameOverride
        credentialsFileNameOverride = builder.credentialsFileNameOverride
    }

    public class Builder {
        /**
         * Connection bootstrap to use for any network connections made while sourcing credentials.
         */
        public var clientBootstrap: ClientBootstrap? = null

        /**
         * The tls context to use for any secure network connections made while sourcing credentials.
         */
        public var tlsContext: TlsContext? = null

        /**
         * The name of the profile to use (or `"default"` if none is specified).
         */
        public var profileName: String? = null

        /**
         * The name of the config file to use. If none is specified, the default is `".aws/config"` on Linux/Mac and
         * `"%USERPROFILE%\.aws\config"` on Windows.
         */
        public var configFileNameOverride: String? = null

        /**
         * The name of the credentials file to use. If none is specified, the default is `".aws/credentials"` on Linux/Mac
         * and `"%USERPROFILE%\.aws\credentials"` on Windows.
         */
        public var credentialsFileNameOverride: String? = null

        public fun build(): ProfileCredentialsProvider = ProfileCredentialsProvider(this)
    }
}
