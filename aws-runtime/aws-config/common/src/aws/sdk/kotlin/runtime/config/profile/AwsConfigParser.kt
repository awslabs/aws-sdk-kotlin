/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.logging.Logger
import aws.smithy.kotlin.runtime.logging.warn
import aws.smithy.kotlin.runtime.util.PlatformProvider

// Literal characters used in parsing AWS SDK configuration files
internal object Literals {
    const val DEFAULT_PROFILE = "default"
    const val NEW_LINE: Char = '\n'
    const val PROFILE_KEYWORD = "profile"
    const val COMMENT_1 = "#"
    const val COMMENT_2 = ";"
    const val INLINE_COMMENT_1 = " $COMMENT_1"
    const val INLINE_COMMENT_2 = " $COMMENT_2"
    const val PROFILE_PREFIX = '['
    const val PROFILE_SUFFIX = ']'
    const val PROPERTY_SPLITTER = '='
}

/**
 * Profiles are represented as a map of maps.  Each top-level key is a profile.  It's associated
 * entries are the property key-value pairs for that profile.
 */
internal typealias ProfileMap = Map<String, Map<String, String>>

/**
 * Tokens representing state declared in AWS configuration files. Other types such as empty lines
 * or comments are filtered out before parsing.
 */
@InternalSdkApi
internal sealed class Token {
    /**
     * A profile definition line declares that the attributes that follow (until another profile definition is encountered)
     * are part of a named collection of attributes.
     * @property profilePrefix true if section used 'profile' prefix.
     * @property name name of profile
     * @property isValidForm true if the declaration is valid, false if declaration is not compatible with an associated [FileType].
     */
    data class Profile(val profilePrefix: Boolean, val name: String, val isValidForm: Boolean = true) : Token()
    /**
     * Represents a property definition line in an AWS configuration file.
     * @property key key of property
     * @property value value of property
     */
    data class Property(val key: String, val value: String) : Token()
    /**
     * Represents a line that is not a profile or property definition.
     * @property line string literal of line that could not be parsed.
     */
    data class Unmatched(val line: FileLine) : Token()
}

/**
 * This type specifies the behavior of each configuration file type.
 *
 * @property setting The [AwsSdkSetting] from which the file location can be determined.
 * @property lineParsers A list of line parsers associated with the configuration file type.
 *
 * Two files are used for SDK configuration: a configuration file and a credentials file. Both files are UTF-8 encoded
 * and support all SDK configuration parameters, but have slight format differences.
 *
 * File	            Default Location	            Location Environment Variable
 * ------------------------------------------------------------------------------
 * Configuration	~/.aws/config	                AWS_CONFIG_FILE
 * Credentials	    ~/.aws/credentials	            AWS_SHARED_CREDENTIALS_FILE
 */
internal enum class FileType(
    private val setting: AwsSdkSetting<String>,
    private val lineParsers: List<ParseFn>,
    private val pathSegments: List<String>
) {
    CONFIGURATION(
        AwsSdkSetting.AwsConfigFile,
        listOf(::configurationProfile, ::property, ::unmatchedLine),
        listOf("~", ".aws", "config")
    ),
    CREDENTIAL(
        AwsSdkSetting.AwsSharedCredentialsFile,
        listOf(::credentialProfile, ::property, ::unmatchedLine),
        listOf("~", ".aws", "credentials")
    );

    /**
     * Determine the absolute path of the configuration file based on environment and policy
     * @return the absolute path of the configuration file. This does not imply the file exists or is otherwise valid
     */
    fun path(platform: PlatformProvider): String =
        setting.resolve(platform)?.trim() ?: pathSegments.joinToString(separator = platform.filePathSeparator)

    /**
     * Parse a line into a token.  A file may contain the following line types:
     *
     * Term	                    Definition	                                                Examples
     * Profile Definition	    A line that applies a name to a set of properties	        [default], [profile my-profile]
     * Property Definition	    A piece of configuration applied to a profile	            aws_access_key_id = ACCESS_KEY_0
     * Property Continuation	The continuation of a value started in property definition	max_concurrent_requests = 20
     * Comment Line	            A human-readable line of text, ignored by implementation	# My Profile
     * Blank Line	            A whitespace-only line, ignored by the implementation
     *
     * Only Profile and Property Definition lines are modeled as tokens.  Property continuations are collapsed
     * into Property Definition lines via the [mergeContinuations] function and comment and blank lines are discarded.
     *
     * This function works on a parse function list heuristic, which is a list of functions that
     * return an [Token] instance on successful parse or null if failed.  Each file type has a particular parse chain.
     *
     * Unknown Line Type
     * In the event that a line cannot be determined to be one of the types documented above, parsing of the profile must fail immediately.
     *
     * The following examples constitute unknown line types:
     *
     * A profile definition without a closing ]
     * A property definition without an =
     * A sub-property definition without an =
     */
    fun tokenOf(input: FileLine): Token =
        lineParsers.firstNotNullOf { parseFunction -> parseFunction(input) }
}

// Base exception for AWS config file parser errors.
public open class AwsConfigParseException(message: String) : ConfigurationException(message)

// Describes a function that attempts to parse a line into a Token or returns null on failure
private typealias ParseFn = (FileLine) -> Token?

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

    val tokenMap = buildMap<Token.Profile, Map<String, String>> {
        var lastProfile: Token.Profile? = null

        mergeContinuations(input)
            .filter { it.content.isNotEmpty() }
            .map { FileLine(it.lineNumber, it.content.stripInlineComments()) }
            .map { type.tokenOf(it) to it.lineNumber }
            .forEach { (token, lineNumber) ->
                when (token) {
                    is Token.Profile -> {
                        lastProfile = token
                        if (!containsKey(token) && token.isValidForm) put(token, emptyMap()) // Discard malformed profiles
                    }
                    is Token.Property -> {
                        val outerMap = this
                        if (lastProfile == null) throwParseException("Expected a profile definition preceding '${token.key}'", lineNumber)

                        // Profile definitions in configuration files without the profile prefix are silently dropped.
                        if (lastProfile!!.isValidForm) {
                            put(
                                lastProfile!!,
                                buildMap {
                                    putAll(outerMap[lastProfile]!!)
                                    if (containsKey(token.key)) {
                                        warn("'${token.key}' defined multiple times in profile '${lastProfile?.name}'", lineNumber)
                                    }
                                    put(token.key, token.value)
                                }
                            )
                        } else {
                            warn("Ignoring property '${token.key}' because '${lastProfile?.name}' is an invalid profile", lineNumber)
                        }
                    }
                    is Token.Unmatched -> { warn("Ignoring unknown data", lineNumber) }
                }
            }
    }

    return mergeProfiles(tokenMap)
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
private fun mergeProfiles(tokenIndexMap: Map<Token.Profile, Map<String, String>>): ProfileMap =
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

private val logger = Logger.getLogger("AwsConfigParser")
private const val helpText = "See https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-configure.html for file format details."
/**
 * Format (Configuration Files): [ Whitespace? profile Whitespace Identifier Whitespace? ] Whitespace? CommentLine?
 * Components: A profile line consists of brackets, and a profile name: [profile-name]. A comment line can be appended
 * after the value, with or without whitespace separating it from the profile definition.
 *
 * Component	Description
 * Brackets	    The [ and ] characters at the beginning and end of the profile definition
 * Profile      Name	An identifier that can be referenced when the SDK is used to refer to this group of properties
 *
 * In configuration files, the profile name must start with profile. (eg. [profile profile-name]), except where the
 * profile name is default. When the profile name is default it may start with profile.
 * (eg. [profile default] or [default]).
 * Any amount of whitespace is permitted between profile and the profile name.
 *
 * Invalid Profile Name
 * In the event of an invalid profile name, the entire profile and its properties must be ignored.
 */
private fun configurationProfile(input: FileLine): Token.Profile? =
    input
        .content
        .stripComment(Literals.COMMENT_1)
        .stripComment(Literals.COMMENT_2)
        .trim()
        .verifyProfileWrapper(input.lineNumber)
        .stripOuterOrNull(Literals.PROFILE_PREFIX to Literals.PROFILE_SUFFIX)
        ?.trim()
        ?.let { line ->
            val (profileName, profilePrefix) = when {
                line.isProfileKeyword() -> line.stripProfilePrefix() to true
                line == Literals.DEFAULT_PROFILE -> line to false
                // Return profile with invalid form rather than throw exception because invalid profiles should be ignored.
                else -> {
                    logger.warn("Ignoring invalid profile: '$line' on line ${input.lineNumber}. $helpText")
                    return@let Token.Profile(false, line, false)
                }
            }

            Token.Profile(profilePrefix, profileName, profileName.isContiguous())
        }

/**
 * Format (Credentials Files): [ Whitespace? Identifier Whitespace? ] Whitespace? CommentLine?
 * Components: A profile line consists of brackets, and a profile name: [profile-name]. A comment line can be appended
 * after the value, with or without whitespace separating it from the profile definition.
 *
 * Component	Description
 * Brackets	    The [ and ] characters at the beginning and end of the profile definition
 * Profile      Name	An identifier that can be referenced when the SDK is used to refer to this group of properties
 *
 * In credentials files, the profile name must not start with profile. (eg. [profile-name]).
 */
private fun credentialProfile(input: FileLine): Token.Profile? =
    input
        .content
        .stripComment(Literals.COMMENT_1)
        .stripComment(Literals.COMMENT_2)
        .trim()
        .verifyProfileWrapper(input.lineNumber)
        .stripOuterOrNull(Literals.PROFILE_PREFIX to Literals.PROFILE_SUFFIX)
        ?.trim()
        ?.let { line -> Token.Profile(false, line, line.isContiguous()) }

/**
 * Property Definition Line
 * A property is an attribute configured in a particular profile. The set of supported attributes is discussed later.
 *
 * Format: Identifier Whitespace? = Whitespace? Value? Whitespace? (Whitespace CommentLine)?
 * Components: A property definition line consists of a key, an equals sign and a value: key = value. A comment line
 * can be appended after the value, but it must be prepended with whitespace to separate it from the rest of the value.
 *
 * Component	    Description
 * Property Key	    An identifier defined by the SDK as a supported setting
 * Equals Sign	    The = between the property key and value
 * Property Value	The customer-defined value for the setting
 *
 * Example	                                                        File
 * Declaring a property key with value my value	                    key = my value
 * Declaring a commented property key with value my value	        key = my value ; Comment
 * Declaring a property key with value my value; Failed Comment	    key = my value; Failed Comment
 * Declaring a property with extra whitespace	                    key \t = \t my value \t
 * Declaring a property with minimal spacing	                    key=my value
 *
 * If a blank (empty valued) property is followed by a property continuation, the property continuation is considered
 * to be a sub-property of the blank property. In this case, the property continuation line must be parsed in the same
 * manner as a Property Definition, except that comments are also considered part of the sub-property's value.
 * For example, s3 = followed by the line max_concurrent_requests = 30 defines the value of the s3 property to be a
 * mapping from max_concurrent_requests to 30.
 */
private fun property(input: FileLine): Token.Property? {
    if (input.content.isContinuationLine() || input.content.isProfileLine()) return null

    val (key, value) = input.content.splitProperty(input.lineNumber)

    return if (key.isContiguous()) {
        Token.Property(key, value)
    } else {
        logger.warn("Invalid property key: '$key' on line ${input.lineNumber}. $helpText")
        null
    }
}

/*
 This function is called when a given line does not map to a Profile or Property definition.
 We return a type to model this un-parsable data rather than throw some kind of parse exception
 due to the expected behavior of the parser, in that in particular cases it should ignore bad
 input and in other cases it should throw exceptions.  See [AwsProfileParserTest] for examples.
 */
private fun unmatchedLine(input: FileLine): Token = Token.Unmatched(input)

// See above regarding Unknown Line Type cases
private fun String.verifyProfileWrapper(lineNumber: Int): String {
    // Profile definitions without brackets cause parsing to fail immediately.
    if (startsWith(Literals.PROFILE_PREFIX) && !endsWith(Literals.PROFILE_SUFFIX))
        throwParseException("Profile definition must end with '${Literals.PROFILE_SUFFIX}'", lineNumber)

    return this
}

// Remove the specified wrapper chars and return substring if exist or null if wrapper chars do not match
internal fun String.stripOuterOrNull(outer: Pair<Char, Char>) =
    if (isEmpty()) null else if (first() == outer.first && last() == outer.second) substring(1, length - 1) else null

/**
 * Comment Behavior
 * Both ; and # are supported for defining a comment.
 * In profile definitions, ; and # define a comment, even if they are adjacent to the closing bracket.
 * In property values, ; and # define a comment only if they are preceded by whitespace.
 * In property values, ; and # and all following content are included in the value if they are not preceded by whitespace.
 * In property continuations, ; and # do not define a comment, and they are included in the continued values.
 */
// Strip all inline comments
private fun String.stripInlineComments() = stripComment(Literals.INLINE_COMMENT_1).stripComment(Literals.INLINE_COMMENT_2)

// Strip comment if a newline is not present.  Newline indicates a merged property definition.
// Comments are considered part of the sub-property's value
private fun String.stripComment(comment: String) =
    if (contains(Literals.NEW_LINE)) this else split(comment, limit = 2)[0]

// Parse a property definition line into a key and value pair.
private fun String.splitProperty(lineNumber: Int): Pair<String, String> {
    val kv = split(Literals.PROPERTY_SPLITTER, limit = 2)
    if (kv.size != 2) throwParseException("Expected an ${Literals.PROPERTY_SPLITTER} sign defining a property", lineNumber)
    if (kv[0].isBlank()) throwParseException("Property did not have a name", lineNumber)

    val key = kv[0].trim()
    val value = if (kv[1].isPropertyContinuation()) kv[1] else kv[1].trim() // if property continuation, do not remove whitespace

    return key to value
}

// true if line is a merged property continuation
private fun String.isPropertyContinuation(): Boolean = isNotEmpty() && first() == Literals.NEW_LINE

// A property name is considered invalid if the property definition key includes invalid characters, such as spaces.
private fun String.isContiguous(): Boolean = !any { char -> char.isWhitespace() }

// for a String matching the pattern"profile<whitespace?>*", return string after
private fun String.stripProfilePrefix() = substring(Literals.PROFILE_KEYWORD.length + 1).trim()

// Matches profile keyword and postfix whitespace
private fun String.isProfileKeyword(): Boolean =
    startsWith(Literals.PROFILE_KEYWORD) &&
        (getOrNull(Literals.PROFILE_KEYWORD.length)?.isWhitespace() ?: false)

// If both elements of the pair are non-null, return them concatenated.  Otherwise, return null.
internal fun Pair<String?, String?>.concatOrNull() = if (first != null && second != null) first + second else null

/**
 * Replicates experimental function of the same name.  Usage allows for not opting into experimental APIs
 *
 * TODO: When/if the stdlib version becomes stable this should be removed
 */
internal fun <K, V> buildMap(block: MutableMap<K, V>.() -> Unit): Map<K, V> = mutableMapOf<K, V>().apply(block)

private fun contextMessage(message: String, lineNumber: Int): String =
    "$message on line $lineNumber. $helpText"

private fun warn(message: String, lineNumber: Int) {
    logger.warn(contextMessage(message, lineNumber))
}

private fun throwParseException(message: String, lineNumber: Int): Nothing {
    throw AwsConfigParseException(contextMessage(message, lineNumber))
}
