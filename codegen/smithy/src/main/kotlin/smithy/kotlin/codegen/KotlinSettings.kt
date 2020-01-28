package smithy.kotlin.codegen

import smithy.kotlin.codegen.utils.getLogger
import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.node.StringNode
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.ProtocolsTrait
import kotlin.streams.toList

private val LOGGER = getLogger<KotlinSettings>()

private const val PACKAGE = "package"
private const val PACKAGE_DESCRIPTION = "packageDescription"
private const val PACKAGE_VERSION = "packageVersion"
private const val SERVICE = "service"
private const val PROTOCOL = "protocol"
private const val TARGET_NAMESPACE = "targetNamespace"

class KotlinSettings private constructor(
    val packageName: String,
    val packageVersion: String,
    val packageDescription: String,
    val service: ShapeId,
    val protocol: String?,
    val pluginSettings: ObjectNode
) {
    fun getService(model: Model): ServiceShape = model
        .getShape(service).orElseThrow { CodegenException("Service shape not found: $service") }
        .asServiceShape().orElseThrow { CodegenException("Shape is not a Service: $service") }

    /**
     * Resolves the highest priority protocol from a service shape that is
     * supported by the generator.
     *
     * @param service Service to get the protocols from if "protocols" is not set.
     * @param supportedProtocols The set of protocol names supported by the generator.
     * @return Returns the resolved protocol name.
     * @throws UnresolvableProtocolException if no protocol could be resolved.
     */
    fun resolveServiceProtocol(service: ServiceShape, supportedProtocols: Set<String>): String {
        if (protocol?.isNotEmpty() == true) {
            return protocol
        }

        val resolvedProtocols = service.getTrait(ProtocolsTrait::class.java)
            .map { it.protocolNames }
            .orElseThrow {
                UnresolvableProtocolException(
                    "Unable to derive the protocol setting of the service `${service.id}` because no "
                            + "`@protocols` trait was set. You need to set an explicit `protocol` to generate in "
                            + "smithy-build.json to generate this service."
                )
            }

        return resolvedProtocols.firstOrNull { supportedProtocols.contains(it) } ?: throw UnresolvableProtocolException(
            "The ${service.id} service supports the following unsupported protocols $resolvedProtocols. " +
                    "The following protocol " + "generators were found on the class path: $supportedProtocols"
        )
    }

    companion object {
        fun configure(model: Model, config: ObjectNode): KotlinSettings {
            config.warnIfAdditionalProperties(
                listOf(
                    PACKAGE,
                    PACKAGE_DESCRIPTION,
                    PACKAGE_VERSION,
                    SERVICE,
                    TARGET_NAMESPACE
                )
            )

            val packageName = config.expectStringMember(PACKAGE).value
            return KotlinSettings(
                packageName = packageName,
                packageVersion = config.expectStringMember(PACKAGE_VERSION).value,
                packageDescription = config.getStringMemberOrDefault(PACKAGE_DESCRIPTION, "$packageName client"),
                protocol = config.getStringMemberOrDefault(PROTOCOL, null),
                service = config.getStringMember(SERVICE)
                    .map(StringNode::expectShapeId)
                    .orElseGet { inferService(model) },
                pluginSettings = config
            )
        }

        private fun inferService(model: Model): ShapeId {
            val services = model
                .shapes(ServiceShape::class.java)
                .map { it.id }
                .sorted()
                .toList()

            if (services.isEmpty()) {
                throw CodegenException("Cannot infer a service to generate because the model does not contain any service shapes")
            }
            if (services.size > 1) {
                throw CodegenException("Cannot infer a service to generate because the model contains multiple service shapes: $services")
            }

            val shape = services.first()
            LOGGER.info("Inferring service to generate as $shape")
            return shape
        }
    }
}


