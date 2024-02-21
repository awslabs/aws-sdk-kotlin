/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.telemetry.logging.warn

// Map keyed by section type to map of sections keyed by name
internal typealias TypedSectionMap = Map<ConfigSectionType, SectionMap>

// map keyed by name to config section
internal typealias SectionMap = Map<String, ConfigSection>

internal fun TypedSectionMap.toSharedConfig(source: AwsConfigurationSource): AwsSharedConfig = AwsSharedConfig(this, source)

/**
 * Base exception for AWS config file parser errors.
 */
public class AwsConfigParseException(message: String, lineNumber: Int) : ConfigurationException(contextMessage(message, lineNumber))

/**
 * Parse an AWS configuration file into sections
 *
 * @param type The type of file to parse
 * @param input The payload to parse
 * @return map of section name to section
 */
internal fun parse(logger: Logger, type: FileType, input: String?): TypedSectionMap {
    // Inaccessible File: If a file is not found or cannot be opened in the configured location, the implementation must
    // treat it as an empty file, and must not attempt to fall back to any other location.
    if (input.isNullOrBlank()) return emptyMap()

    val tokens = tokenize(type, input)
    val sections = tokens.toSectionMap(logger)
    return mergeSections(sections)
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

    var currentSection: Token.Section? = null
    var lastProperty: Token.Property? = null
    for (line in lines) {
        val token = type.tokenOf(line, currentSection, lastProperty)

        if (token is Token.Section) {
            currentSection = token
            lastProperty = null
        } else if (token is Token.Property) {
            lastProperty = token
        }

        add(line to token)
    }
}

/**
 * Convert the contents of a token list into a section mapping
 */
internal fun List<Pair<FileLine, Token>>.toSectionMap(logger: Logger): Map<Token.Section, MutableMap<String, AwsConfigValue>> = buildMap {
    var currentSection: Token.Section? = null
    var currentProperty: Token.Property? = null
    var currentParentMap: MutableMap<String, String>? = null

    for ((line, token) in this@toSectionMap) {
        when (token) {
            is Token.Section -> {
                currentSection = token
                currentProperty = null

                if (containsKey(token)) continue
                if (!token.isValid) {
                    logger.warnParse(line) { "Ignoring invalid ${token.sectionName} '${token.name}'" }
                    continue
                }

                put(token, mutableMapOf())
            }
            is Token.Property -> {
                currentSection as Token.Section
                currentProperty = token

                if (!token.isValid) {
                    logger.warnParse(line) { "Ignoring invalid property '${token.key}'" }
                    continue
                }
                if (!currentSection.isValid) {
                    logger.warnParse(line) { "Ignoring property under invalid ${currentSection.sectionName} '${currentSection.name}'" }
                    continue
                }

                val profile = this[currentSection]!!
                if (profile.containsKey(token.key)) {
                    logger.warnParse(line) { "'${token.key}' defined multiple times in ${currentSection.sectionName} '${currentSection.name}'" }
                }

                if (profile.containsKey(token.key)) {
                    logger.warnParse(line) { "Overwriting previously-defined property '${token.key}'" }
                }
                profile[token.key] = AwsConfigValue.String(token.value)
            }
            is Token.Continuation -> {
                currentSection as Token.Section
                currentProperty as Token.Property

                val profile = this[currentSection]!!
                val currentValue = (profile[currentProperty.key] as AwsConfigValue.String).value
                profile[currentProperty.key] = AwsConfigValue.String(currentValue + "\n" + token.value)
            }
            is Token.SubProperty -> {
                currentSection as Token.Section
                currentProperty as Token.Property

                if (!token.isValid) {
                    logger.warnParse(line) { "Ignoring invalid sub-property '${token.key}'" }
                    continue
                }

                val profile = this[currentSection]!!
                val property = profile[currentProperty.key]
                if (property is AwsConfigValue.String) { // convert newly recognized parent to map
                    if (property.value.isNotEmpty()) {
                        logger.warnParse(line) { "Overwriting previously-defined property '${token.key}'" }
                    }
                    currentParentMap = mutableMapOf()
                    profile[currentProperty.key] = AwsConfigValue.Map(currentParentMap)
                }

                currentParentMap!![token.key] = token.value
            }
        }
    }
}

private val Token.Section.sectionName: String
    get() = when (type) {
        ConfigSectionType.PROFILE -> Literals.PROFILE_KEYWORD
        ConfigSectionType.SSO_SESSION -> Literals.SSO_SESSION_KEYWORD
        ConfigSectionType.SERVICES -> Literals.SERVICES_KEYWORD
        ConfigSectionType.UNKNOWN -> "unknown section"
    }

/**
 * When inputs have mixed section prefixes, drop those without the prefix.
 *
 * Duplication Handling
 *
 * Sections duplicated within the same file have their properties merged.
 * If both [profile foo] and [foo] are specified in the same file, their properties are NOT merged.
 * If both [profile foo] and [foo] are specified in the configuration file, [profile foo]'s properties are used.
 * Properties duplicated within the same file across sections use the later property in the file.
 */
private fun mergeSections(tokenIndexMap: Map<Token.Section, Map<String, AwsConfigValue>>): TypedSectionMap {
    val allSections = tokenIndexMap
        .filter { entry ->
            when (entry.key.hasSectionPrefix) {
                true -> true
                false -> {
                    val prefixVariantExists = tokenIndexMap.keys.any { it.hasSectionPrefix && it.name == entry.key.name && it.type == entry.key.type }
                    !prefixVariantExists
                }
            }
        }
        .map { entry -> ConfigSection(entry.key.name, entry.value, entry.key.type) }

    val sectionTypeMap = mutableMapOf<ConfigSectionType, SectionMap>()
    ConfigSectionType.values().forEach { sectionType ->
        val sections = allSections.filter { it.sectionType == sectionType }

        val merged = mergeSections(sections)
        if (merged.isNotEmpty()) {
            sectionTypeMap[sectionType] = merged
        }
    }

    return sectionTypeMap
}

private fun mergeSections(sections: List<ConfigSection>): SectionMap = buildMap {
    sections.forEach { section ->
        val existingProps = get(section.name)?.properties ?: emptyMap()

        // favor the later properties
        val merged = existingProps + section.properties
        put(section.name, ConfigSection(section.name, merged, section.sectionType))
    }
}

private inline fun Logger.warnParse(line: FileLine, crossinline content: () -> String) = warn {
    contextMessage(content(), line.lineNumber)
}
