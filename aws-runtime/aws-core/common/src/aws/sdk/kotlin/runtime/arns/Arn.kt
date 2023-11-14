/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.arns

private const val ARN_COMPONENT_COUNT = 6

/**
 * Represents an [Amazon Resource Name (ARN)](https://docs.aws.amazon.com/IAM/latest/UserGuide/reference-arns.html).
 *
 * The following Arn formats are supported:
 *  * `arn:partition:service:region:account-id:resource-id`
 *  * `arn:partition:service:region:account-id:resource-type/resource-id`
 *  * `arn:partition:service:region:account-id:resource-type:resource-id`
 *  * `arn:partition:service:region:account-id:resource-type:resource-id:qualifier`
 *  * `arn:partition:service:region:account-id:resource-type:resource-id/qualifier`
 *
 *  The exact format of an ARN depends on the service and resource type. Some resource ARNs can include a path or
 *  wildcard. To look up the ARN format for a specific AWS resource, open the
 *  [Service Authorization Reference](https://docs.aws.amazon.com/service-authorization/latest/reference/),
 *  open the page for the service, and navigate to the resource types table.
 */
public class Arn(
    public val partition: String,
    public val service: String,
    public val region: String?,
    public val accountId: String?,
    public val resource: ArnResource,
) {
    public companion object {

        public inline operator fun invoke(block: Builder.() -> Unit): Arn = Builder().apply(block).build()

        /**
         * Parse a string into an [Arn]
         */
        public fun parse(arn: String): Arn {
            val parts = arn.split(':', limit = ARN_COMPONENT_COUNT)
            require(parts.size == ARN_COMPONENT_COUNT) { "Malformed ARN ($arn) does not have the expected number of components" }
            require(parts[0] == "arn") { "Malformed ARN - does not start with `arn:`" }
            require(parts[1].isNotBlank()) { "Malformed ARN - no AWS partition specified" }
            require(parts[2].isNotBlank()) { "Malformed ARN - no AWS service specified" }

            return Arn {
                partition = parts[1]
                service = parts[2]
                region = parts[3].takeIf(String::isNotBlank)
                accountId = parts[4].takeIf(String::isNotBlank)
                resource = ArnResource.parse(parts[5])
            }
        }
    }

    internal constructor(builder: Builder) : this(
        builder.partition!!,
        builder.service!!,
        builder.region,
        builder.accountId,
        builder.resource!!,
    )

    init {
        require(region == null || region.isNotBlank()) { "ARN region must not be blank" }
        require(accountId == null || accountId.isNotBlank()) { "ARN accountId must not be blank" }
    }

    override fun toString(): String = buildString {
        append("arn:$partition:$service:")
        if (region != null) {
            append(region)
        }
        append(":")
        if (accountId != null) {
            append(accountId)
        }
        append(":$resource")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Arn) return false
        if (partition != other.partition) return false
        if (service != other.service) return false
        if (region != other.region) return false
        if (accountId != other.accountId) return false
        return resource == other.resource
    }

    override fun hashCode(): Int {
        var result = partition.hashCode()
        result = 31 * result + service.hashCode()
        result = 31 * result + (region?.hashCode() ?: 0)
        result = 31 * result + (accountId?.hashCode() ?: 0)
        result = 31 * result + resource.hashCode()
        return result
    }

    public class Builder {
        public var partition: String? = null
        public var service: String? = null
        public var region: String? = null
        public var accountId: String? = null
        public var resource: ArnResource? = null

        public fun resource(block: ArnResource.Builder.() -> Unit) {
            resource = ArnResource.Builder().apply(block).build()
        }

        @PublishedApi
        internal fun build(): Arn {
            require(!partition.isNullOrBlank()) { "ARN partition must not be null or blank" }
            require(!service.isNullOrBlank()) { "ARN service must not be null or blank" }
            requireNotNull(resource) { "ARN resource must not be null" }
            return Arn(this)
        }
    }
}

/**
 * The separator to use between the `resource-type` and `resource-id` in an ARN
 */
public enum class ArnResourceTypeSeparator(public val separator: String) {
    SLASH("/"),
    COLON(":"),
    ;

    public companion object {
        public fun fromValue(value: String): ArnResourceTypeSeparator = when (value) {
            "/" -> SLASH
            ":" -> COLON
            else -> error("unknown ARN resource type separator `$value`, expected one of ${values().map { it.separator }}")
        }
    }
}

/**
 * Represents the resource portion of an Amazon Resource Name (ARN).
 *
 * Some ARNs use a `/` to separate `resource-type` and `resource-id`, others use a `:`. The following ARN formats
 * are all valid.
 *
 *  * `arn:partition:service:region:account-id:resource-type:resource-id`
 *  * `arn:partition:service:region:account-id:resource-type:resource-id:qualifier`
 *  * `arn:partition:service:region:account-id:resource-type:resource-id/qualifier`
 *
 *  This is controlled by [resourceTypeSeparator].
 */
public class ArnResource(
    public val id: String,
    public val type: String? = null,
    public val resourceTypeSeparator: ArnResourceTypeSeparator = ArnResourceTypeSeparator.COLON,
) {
    internal constructor(builder: Builder) : this(builder.id!!, builder.type, builder.resourceTypeSeparator)

    init {
        require(type == null || type.isNotBlank()) { "ARN resource type must not be blank" }
    }

    public companion object {
        /**
         * Parse an ARN resource from a string. ARN resources support multiple formats which may contain
         * the same delimiters used to separate `<resource-type>` and `<resource-id>`. This may result in
         * the parsed representation interpreting these parts as the resource type incorrectly.
         */
        public fun parse(resource: String): ArnResource {
            // <resource-id> || <resource-type>[:/]<resource-id>
            require(resource.isNotEmpty()) { "ARN resource must not be empty" }

            val delims = charArrayOf(':', '/')
            val firstDelim = resource.indexOfAny(delims)

            val builder = Builder()
            when {
                firstDelim != -1 -> {
                    val delim = resource[firstDelim]
                    val parts = resource.split(delim, limit = 2)
                    builder.type = parts[0]
                    builder.id = parts[1]
                    builder.resourceTypeSeparator = ArnResourceTypeSeparator.fromValue(delim.toString())
                }
                else -> builder.id = resource
            }

            return builder.build()
        }
    }

    override fun toString(): String = buildString {
        if (type != null) {
            append("$type")
            append(resourceTypeSeparator.separator)
        }

        append(id)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (type?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArnResource) return false
        if (id != other.id) return false
        return type == other.type
    }

    public class Builder {
        public var id: String? = null
        public var type: String? = null
        public var resourceTypeSeparator: ArnResourceTypeSeparator = ArnResourceTypeSeparator.COLON

        @PublishedApi
        internal fun build(): ArnResource {
            require(!id.isNullOrBlank()) { "ARN resource id must not be null or blank" }
            return ArnResource(this)
        }
    }
}
