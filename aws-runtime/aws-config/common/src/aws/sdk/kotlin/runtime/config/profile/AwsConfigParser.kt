/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.smithy.kotlin.runtime.logging.Logger
import aws.smithy.kotlin.runtime.logging.warn

internal val logger = Logger.getLogger("AwsConfigParser")

/**
 * Profiles are represented as a map of maps.  Each top-level key is a profile.  Its associated
 * entries are the property key-value pairs for that profile.
 */
internal typealias ProfileMap = Map<String, Map<String, AwsConfigValue>>

/**
 * Base exception for AWS config file parser errors.
 */
public open class AwsConfigParseException(message: String, lineNumber: Int) : ConfigurationException(contextMessage(message, lineNumber))

/**
 * Parse an AWS configuration file
 *
 * @param type The type of file to parse
 * @param input The payload to parse
 */
internal fun parse(type: FileType, input: String?): ProfileMap {
    // Inaccessible File: If a file is not found or cannot be opened in the configured location, the implementation must
    // treat it as an empty file, and must not attempt to fall back to any other location.
    if (input.isNullOrBlank()) return emptyMap()

    val tokens = tokenize(type, input)
    val profiles = tokens.toProfileMap()
    return mergeProfiles(profiles)
}

/**
 * Convert an input file's contents into a list of tokens.
 *
 * The final order of the list has logical relevance w.r.t. the final output, a consumer should not alter or remove
 * values from it when attempting to build a set of profiles.
 */
internal fun tokenize(type: FileType, input: String): List<Token> = buildList {
    val lines = input
        .lines()
        .mapIndexed { index, line -> FileLine(index + 1, line) }
        .filter { it.content.isNotBlank() && !it.isComment() }

    var currentProfile: Token.Profile? = null
    var lastProperty: Token.Property? = null
    for (line in lines) {
        val token = type.tokenOf(line, currentProfile, lastProperty)
            ?: throw AwsConfigParseException("Encountered unexpected token", line.lineNumber)

        if (token is Token.Profile) {
            currentProfile = token
            lastProperty = null
        } else if (token is Token.Property) {
            lastProperty = token
        }

        add(token)
    }
}

/**
 * Convert the contents of a token list into a profile mapping.
 */
internal fun List<Token>.toProfileMap(): Map<Token.Profile, MutableMap<String, AwsConfigValue>> = buildMap {
    var currentProfile: Token.Profile? = null
    var currentProperty: Token.Property? = null
    var currentParentMap: MutableMap<String, String>? = null

    for (token in this@toProfileMap) {
        when (token) {
            is Token.Profile -> {
                currentProfile = token
                currentProperty = null

                if (containsKey(token)) continue
                if (!token.isValidForm) {
                    logger.warn {
                        contextMessage("Ignoring invalid profile '${token.name}'.")
                    }
                    continue
                }

                put(token, mutableMapOf())
            }
            is Token.Property -> {
                currentProfile as Token.Profile
                currentProperty = token

                if (!token.isValid) {
                    logger.warn {
                        contextMessage("Ignoring invalid property '${token.key}'.")
                    }
                    continue
                }
                if (!currentProfile.isValidForm) {
                    logger.warn {
                        contextMessage("Ignoring property under invalid profile '${currentProfile.name}'.")
                    }
                    continue
                }

                val profile = this[currentProfile]!!
                if (profile.containsKey(token.key)) {
                    logger.warn {
                        contextMessage("'${token.key}' defined multiple times in profile '${currentProfile.name}.'")
                    }
                }

                profile[token.key] = AwsConfigValue.String(token.value)
            }
            is Token.Continuation -> {
                currentProfile as Token.Profile
                currentProperty as Token.Property

                val profile = this[currentProfile]!!
                val currentValue = (profile[currentProperty.key] as AwsConfigValue.String).value
                profile[currentProperty.key] = AwsConfigValue.String(currentValue + "\n" + token.value)
            }
            is Token.SubProperty -> {
                currentProfile as Token.Profile
                currentProperty as Token.Property

                if (!token.isValid) {
                    logger.warn {
                        contextMessage("Ignoring invalid sub-property '${token.key}'.")
                    }
                    continue
                }

                val profile = this[currentProfile]!!
                if (profile[currentProperty.key] is AwsConfigValue.String) { // convert newly recognized parent to map
                    currentParentMap = mutableMapOf()
                    profile[currentProperty.key] = AwsConfigValue.Map(currentParentMap)
                }

                currentParentMap!![token.key] = token.value
            }
        }
    }
}

/**
 * When inputs have mixed profile prefixes, drop those without the prefix.
 *
 * Duplication Handling
 *
 * Profiles duplicated within the same file have their properties merged.
 * If both [profile foo] and [foo] are specified in the same file, their properties are NOT merged.
 * If both [profile foo] and [foo] are specified in the configuration file, [profile foo]'s properties are used.
 * Properties duplicated within the same file and profile use the later property in the file.
 */
private fun mergeProfiles(tokenIndexMap: Map<Token.Profile, Map<String, AwsConfigValue>>): ProfileMap =
    tokenIndexMap
        .filter { entry ->
            when (entry.key.profilePrefix) {
                true -> true
                false -> {
                    val prefixVariantExists = tokenIndexMap.containsKey(Token.Profile(true, entry.key.name))
                    !prefixVariantExists
                }
            }
        }
        .mapKeys { entry -> entry.key.name }
