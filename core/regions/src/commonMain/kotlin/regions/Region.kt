package regions

import utils.collections.ConcurrentMap

/**
 * An Amazon Web Services region that hosts a set of Amazon services.
 *
 * An instance of this class can be retrieved by referencing one of the static constants defined in this class (eg.
 * [Region.US_EAST_1]) or by using the [Region.of] method if the region you want is not included in
 * this release of the SDK.
 *
 * Each AWS region corresponds to a separate geographical location where a set of Amazon services is deployed. These
 * regions (except for the special [.AWS_GLOBAL] and [.AWS_CN_GLOBAL] regions) are separate from each other,
 * with their own set of resources. This means a resource created in one region (eg. an SQS queue) is not available in
 * another region.
 *
 * To programmatically determine whether a particular service is deployed to a region, you can use the
 * `serviceMetadata` method on the service's client interface. Additional metadata about a region can be
 * discovered using [RegionMetadata.of].
 *
 * The [id] will be used as the signing region for all requests to AWS services unless an explicit region
 * override is available in [RegionMetadata]. This id will also be used to construct the endpoint for accessing a
 * service unless an explicit endpoint is available for that region in [RegionMetadata].
 *
 */
class Region private constructor(val id: String, val isGlobalRegion: Boolean) {
    private object RegionCache {
        private val VALUES: ConcurrentMap<String, Region> = ConcurrentMap()

        internal fun put(value: String, isGlobalRegion: Boolean): Region = VALUES.computeIfAbsent(value) {
            Region(value, isGlobalRegion)
        }
    }

    companion object {
        val AP_SOUTH_1 = of("ap-south-1")
        val EU_NORTH_1 = of("eu-north-1")
        val EU_WEST_3 = of("eu-west-3")
        val EU_WEST_2 = of("eu-west-2")
        val EU_WEST_1 = of("eu-west-1")
        val AP_NORTHEAST_2 = of("ap-northeast-2")
        val AP_NORTHEAST_1 = of("ap-northeast-1")
        val ME_SOUTH_1 = of("me-south-1")
        val US_GOV_EAST_1 = of("us-gov-east-1")
        val CA_CENTRAL_1 = of("ca-central-1")
        val SA_EAST_1 = of("sa-east-1")
        val AP_EAST_1 = of("ap-east-1")
        val CN_NORTH_1 = of("cn-north-1")
        val US_GOV_WEST_1 = of("us-gov-west-1")
        val AP_SOUTHEAST_1 = of("ap-southeast-1")
        val AP_SOUTHEAST_2 = of("ap-southeast-2")
        val US_ISO_EAST_1 = of("us-iso-east-1")
        val EU_CENTRAL_1 = of("eu-central-1")
        val US_EAST_1 = of("us-east-1")
        val US_EAST_2 = of("us-east-2")
        val US_WEST_1 = of("us-west-1")
        val CN_NORTHWEST_1 = of("cn-northwest-1")
        val US_ISOB_EAST_1 = of("us-isob-east-1")
        val US_WEST_2 = of("us-west-2")
        val AWS_GLOBAL = of("aws-global", true)
        val AWS_CN_GLOBAL = of("aws-cn-global", true)
        val AWS_US_GOV_GLOBAL = of("aws-us-gov-global", true)
        val AWS_ISO_GLOBAL = of("aws-iso-global", true)
        val AWS_ISO_B_GLOBAL = of("aws-iso-b-global", true)

        fun of(value: String): Region = of(value, false)

        private fun of(value: String, isGlobalRegion: Boolean): Region = RegionCache.put(value, isGlobalRegion)
    }
}
