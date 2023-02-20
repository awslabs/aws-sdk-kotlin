/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.profile

/**
 * Tokens representing state declared in AWS configuration files. Other types such as empty lines
 * or comments are filtered out before parsing.
 */
internal sealed interface Token {
    /**
     * Whether the token is well-formed. A token may be recognizable, but its attributes may be syntactically invalid
     * in such a way that it is suppressed from the parsed profile.
     */
    val isValid: Boolean
        get() = true

    /**
     * A profile definition line declares that the attributes that follow (until another profile definition is encountered)
     * are part of a named collection of attributes.
     * @property profilePrefix true if section used 'profile' prefix.
     * @property name name of profile
     * @property isValidForm whether the profile declaration is well-formed within the context of the file type from
     * from where it was parsed
     */
    data class Profile(val profilePrefix: Boolean, val name: String, val isValidForm: Boolean = true) : Token {
        override val isValid: Boolean
            get() = isValidForm
    }

    /**
     * Represents a property definition line in an AWS configuration file.
     * @property key key of property
     * @property value value of property
     */
    data class Property(val key: String, val value: String) : Token {
        override val isValid: Boolean
            get() = key.isValidIdentifier()
    }

    /**
     * Represents a property value continuation.
     * @property value value of continuation
     */
    data class Continuation(val value: String) : Token

    /**
     * Represents a sub-property. A continuation is tokenized as a sub-property when it matches the syntax of both a
     * continuation AND a property definition, AND the value of the property before it is empty.
     * @property key key of sub-property
     * @property value value of sub-property
     */
    data class SubProperty(val key: String, val value: String) : Token {
        override val isValid: Boolean
            get() = key.isValidIdentifier()
    }
}
