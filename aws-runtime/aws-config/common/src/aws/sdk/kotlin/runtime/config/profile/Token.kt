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
     * A section definition line declares that the attributes that follow (until another section definition is encountered)
     * are part of a named collection of attributes.
     * @property name name of the section
     * @param type the type of section
     * @param hasSectionPrefix true if there was a section prefix (e.g. `profile`, `sso-session`)
     * @property isValid the section declaration is well-formed within the context of the file type from where it was parsed
     */
    data class Section(val name: String, val type: ConfigSectionType, val hasSectionPrefix: Boolean, override val isValid: Boolean = true) : Token

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
