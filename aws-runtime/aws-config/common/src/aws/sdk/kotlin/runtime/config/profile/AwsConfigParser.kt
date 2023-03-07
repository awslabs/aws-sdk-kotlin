/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.tracing.*

/**
 * Maps a set of loaded profiles (keyed by name) to their respective [AwsProfile]s.
 */
@InternalSdkApi
public typealias AwsProfiles = Map<String, AwsProfile>

/**
 * Profiles are represented as a map of maps while parsing.  Each top-level key is a profile.  Its associated
 * entries are the property key-value pairs for that profile.
 */
internal typealias RawProfileMap = Map<String, Map<String, AwsConfigValue>>

internal fun RawProfileMap.toProfileMap(): AwsProfiles = mapValues { (k, v) -> AwsProfile(k, v) }

/**
 * Base exception for AWS config file parser errors.
 */
public class AwsConfigParseException(message: String, lineNumber: Int) : ConfigurationException(contextMessage(message, lineNumber))

/**
 * Parse an AWS configuration file
 *
 * @param type The type of file to parse
 * @param input The payload to parse
 */
internal fun parse(traceSpan: TraceSpan, type: FileType, input: String?): RawProfileMap {
    // Inaccessible File: If a file is not found or cannot be opened in the configured location, the implementation must
    // treat it as an empty file, and must not attempt to fall back to any other location.
    if (input.isNullOrBlank()) return emptyMap()

    val tokens = tokenize(type, input)
    val profiles = tokens.toProfileMap(traceSpan)
    return mergeProfiles(profiles)
}

/**
 * Convert an input file's contents into a list of tokens.
 *
 * The final order of the list has logical relevance w.r.t. the final output, a consumer should not alter or remove
 * values from it when attempting to build a set of profiles.
 */
internal fun tokenize(type: FileType, input: String): List<Pair<FileLine, Token>> = buildList {
    val lines = input
        .lines()
        .mapIndexed { index, line -> FileLine(index + 1, line) }
        .filter { it.content.isNotBlank() && !it.isComment() }

    var currentProfile: Token.Profile? = null
    var lastProperty: Token.Property? = null
    for (line in lines) {
        val token = type.tokenOf(line, currentProfile, lastProperty)

        if (token is Token.Profile) {
            currentProfile = token
            lastProperty = null
        } else if (token is Token.Property) {
            lastProperty = token
        }

        add(line to token)
    }
}

/**
 * Convert the contents of a token list into a profile mapping.
 */
internal fun List<Pair<FileLine, Token>>.toProfileMap(traceSpan: TraceSpan): Map<Token.Profile, MutableMap<String, AwsConfigValue>> = buildMap {
    var currentProfile: Token.Profile? = null
    var currentProperty: Token.Property? = null
    var currentParentMap: MutableMap<String, String>? = null

    for ((line, token) in this@toProfileMap) {
        when (token) {
            is Token.Profile -> {
                currentProfile = token
                currentProperty = null

                if (containsKey(token)) continue
                if (!token.isValid) {
                    traceSpan.warnParse(line) { "Ignoring invalid profile '${token.name}'" }
                    continue
                }

                put(token, mutableMapOf())
            }
            is Token.Property -> {
                currentProfile as Token.Profile
                currentProperty = token

                if (!token.isValid) {
                    traceSpan.warnParse(line) { "Ignoring invalid property '${token.key}'" }
                    continue
                }
                if (!currentProfile.isValid) {
                    traceSpan.warnParse(line) { "Ignoring property under invalid profile '${currentProfile.name}'" }
                    continue
                }

                val profile = this[currentProfile]!!
                if (profile.containsKey(token.key)) {
                    traceSpan.warnParse(line) { "'${token.key}' defined multiple times in profile '${currentProfile.name}'" }
                }

                if (profile.containsKey(token.key)) {
                    traceSpan.warnParse(line) { "Overwriting previously-defined property '${token.key}'" }
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
                    traceSpan.warnParse(line) { "Ignoring invalid sub-property '${token.key}'" }
                    continue
                }

                val profile = this[currentProfile]!!
                val property = profile[currentProperty.key]
                if (property is AwsConfigValue.String) { // convert newly recognized parent to map
                    if (property.value.isNotEmpty()) {
                        traceSpan.warnParse(line) { "Overwriting previously-defined property '${token.key}'" }
                    }
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
private fun mergeProfiles(tokenIndexMap: Map<Token.Profile, Map<String, AwsConfigValue>>): RawProfileMap =
    tokenIndexMap
        .filter { entry ->
            when (entry.key.profilePrefix) {
                true -> true
                false -> {
                    val prefixVariantExists = tokenIndexMap.keys.any { it.profilePrefix && it.name == entry.key.name }
                    !prefixVariantExists
                }
            }
        }
        .mapKeys { entry -> entry.key.name }

private inline fun TraceSpan.warnParse(line: FileLine, crossinline content: () -> String) = warn("AwsConfigParser") {
    contextMessage(content(), line.lineNumber)
}
