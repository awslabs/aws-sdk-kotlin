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
internal class Arn(
    public val partition: String,
    public val service: String,
    public val region: String?,
    public val accountId: String?,
    public val resource: String,
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
                resource = parts[5]
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
        public var resource: String? = null

        @PublishedApi
        internal fun build(): Arn {
            require(!partition.isNullOrBlank()) { "ARN partition must not be null or blank" }
            require(!service.isNullOrBlank()) { "ARN service must not be null or blank" }
            requireNotNull(resource) { "ARN resource must not be null" }
            return Arn(this)
        }
    }
}
