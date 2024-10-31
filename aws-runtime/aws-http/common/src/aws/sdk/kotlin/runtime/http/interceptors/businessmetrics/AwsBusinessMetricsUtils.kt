package aws.sdk.kotlin.runtime.http.interceptors.businessmetrics

import aws.sdk.kotlin.runtime.http.BUSINESS_METRICS_MAX_LENGTH
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.copy
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.emitBusinessMetric
import aws.smithy.kotlin.runtime.collections.MutableAttributes
import aws.smithy.kotlin.runtime.collections.toMutableAttributes
import kotlin.jvm.JvmName

/**
 * Makes sure the metrics do not exceed the maximum size and truncates them if so.
 */
internal fun formatMetrics(metrics: MutableSet<String>): String {
    if (metrics.isEmpty()) return ""
    val metricsString = metrics.joinToString(",", "m/")
    val metricsByteArray = metricsString.encodeToByteArray()

    if (metricsByteArray.size <= BUSINESS_METRICS_MAX_LENGTH) return metricsString

    val lastCommaIndex = metricsByteArray
        .sliceArray(0 until 1024)
        .indexOfLast { it == ','.code.toByte() }
        .takeIf { it != -1 }

    lastCommaIndex?.let {
        return metricsByteArray.decodeToString(
            0,
            lastCommaIndex,
            true,
        )
    }

    throw IllegalStateException("Business metrics are incorrectly formatted: $metricsString")
}

/**
 * AWS SDK specific business metrics
 */
@InternalApi
public enum class AwsBusinessMetric(public override val identifier: String) : BusinessMetric {
    S3_EXPRESS_BUCKET("J"),
    ;

    @InternalApi
    public enum class Credentials(public override val identifier: String) : BusinessMetric {
        CREDENTIALS_CODE("e"),
        CREDENTIALS_JVM_SYSTEM_PROPERTIES("f"),
        CREDENTIALS_ENV_VARS("g"),
        CREDENTIALS_ENV_VARS_STS_WEB_ID_TOKEN("h"),
        CREDENTIALS_STS_ASSUME_ROLE("i"),
        CREDENTIALS_STS_ASSUME_ROLE_WEB_ID("k"),
        CREDENTIALS_PROFILE("n"),
        CREDENTIALS_PROFILE_SOURCE_PROFILE("o"),
        CREDENTIALS_PROFILE_NAMED_PROVIDER("p"),
        CREDENTIALS_PROFILE_STS_WEB_ID_TOKEN("q"),
        CREDENTIALS_PROFILE_SSO("r"),
        CREDENTIALS_SSO("s"),
        CREDENTIALS_PROFILE_SSO_LEGACY("t"),
        CREDENTIALS_SSO_LEGACY("u"),
        CREDENTIALS_PROFILE_PROCESS("v"),
        CREDENTIALS_PROCESS("w"),
        CREDENTIALS_HTTP("z"),
        CREDENTIALS_IMDS("0"),
    }
}

/**
 * Emits a business metric into [Credentials.attributes]
 * @param identifier The identifier of the [BusinessMetric] to be emitted.
 */
@InternalApi
public fun Credentials.emitBusinessMetric(identifier: String): Credentials =
    when (val credentialsAttributes = this.attributes) {
        is MutableAttributes -> {
            credentialsAttributes.emitBusinessMetric(identifier)
            this
        }
        else -> {
            val newCredentialsAttributes = credentialsAttributes.toMutableAttributes()
            newCredentialsAttributes.emitBusinessMetric(identifier)
            this.copy(attributes = newCredentialsAttributes)
        }
    }

/**
 * Emits a business metric into [Credentials.attributes]
 * @param metric The [BusinessMetric] to be emitted.
 */
@InternalApi
public fun Credentials.emitBusinessMetric(metric: BusinessMetric): Credentials = this.emitBusinessMetric(metric.identifier)

/**
 * Emits business metrics into [Credentials.attributes]
 * @param identifiers The identifiers of the [BusinessMetric]s to be emitted.
 */
@InternalApi
@JvmName("emitBusinessMetricsWithSetOfString")
public fun Credentials.emitBusinessMetrics(identifiers: Set<String>): Credentials {
    var credentials = this
    identifiers.forEach { identifier ->
        credentials = this.emitBusinessMetric(identifier)
    }
    return credentials
}

/**
 * Emits business metrics into [Credentials.attributes]
 * @param metrics The [BusinessMetric]s to be emitted.
 */
@InternalApi
@JvmName("emitBusinessMetricsWithSetOfBusinessMetrics")
public fun Credentials.emitBusinessMetrics(metrics: Set<BusinessMetric>): Credentials =
    this.emitBusinessMetrics(
        metrics.map { it.identifier }.toSet(),
    )
