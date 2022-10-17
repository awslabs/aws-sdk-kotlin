package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.logging.Logger
import aws.smithy.kotlin.runtime.serde.json.JsonDeserializer
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.Platform
import aws.smithy.kotlin.runtime.util.PlatformProvider

internal expect suspend fun executeCommand(command: String, platformProvider: PlatformProvider): Pair<Int, String>

private const val PROVIDER_NAME = "Process"

/**
 * [CredentialsProvider] that invokes a command to retrieve [Credentials] from stdout.
 * If the exit code is non-zero, log and throw an exception including the stderr from the command.
 *
 * @param credentialProcess the command to invoke to retrieve credentials
 */
public class ProcessCredentialsProvider(
    /**
     * The command to invoke to retrieve credentials
     */
    private val credentialProcess: String,

    /**
     * The platform provider
     */
    private val platformProvider: PlatformProvider = Platform,
) : CredentialsProvider {
    override suspend fun getCredentials(): Credentials {
        val logger = Logger.getLogger<ProcessCredentialsProvider>()

        val (exitCode, output) = try {
            executeCommand(credentialProcess, platformProvider)
        } catch (ex: Exception) {
            throw CredentialsProviderException("Failed to execute command", ex)
        }

        if (exitCode != 0) {
            logger.warn { "Command completed with nonzero exit code $exitCode: $output" }
            throw CredentialsProviderException("Command completed with nonzero exit code $exitCode: $output")
        }

        val payload = output.encodeToByteArray()
        val deserializer = JsonDeserializer(payload)

        return when (val resp = deserializeJsonProcessCredentials(deserializer)) {
            is JsonCredentialsResponse.SessionCredentials -> Credentials(
                resp.accessKeyId,
                resp.secretAccessKey,
                resp.sessionToken,
                resp.expiration,
                PROVIDER_NAME,
            )
            is JsonCredentialsResponse.NonExpiringCredentials -> Credentials(
                resp.accessKeyId,
                resp.secretAccessKey,
                resp.sessionToken,
                Instant.MAX_VALUE,
                PROVIDER_NAME,
            )
            is JsonCredentialsResponse.Error -> {
                throw CredentialsProviderException("Error parsing credentials from process: code=${resp.code}; ${resp.message}")
            }
        }
    }
}
