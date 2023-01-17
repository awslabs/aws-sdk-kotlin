package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.logging.Logger
import aws.smithy.kotlin.runtime.serde.json.JsonDeserializer
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.Platform
import aws.smithy.kotlin.runtime.util.PlatformProvider

internal expect suspend fun executeCommand(
    command: String,
    platformProvider: PlatformProvider,
    maxOutputLengthBytes: Long,
    timeoutMillis: Long,
    clock: Clock = Clock.System,
): Pair<Int, String>

private const val PROVIDER_NAME = "Process"

/**
 * [CredentialsProvider] that invokes a command to retrieve [Credentials] from stdout.
 * If the exit code is non-zero, log and throw an exception including the stderr from the command.
 * 
 * WARNING: This [CredentialsProvider] sources credentials from an external process.
 * This can potentially be dangerous, so proceed with caution. Other credential providers should be preferred if at all possible.
 * If using this option, you should make sure that the config file and any process or script files are as locked down as possible using
 * security best practices for your operating system.
 * @see <a href="https://docs.aws.amazon.com/sdkref/latest/guide/feature-process-credentials.html">Process Credentials</a>
 *
 * @param credentialProcess the command to invoke to retrieve credentials
 * @param platformProvider the platform provider
 * @param maxOutputLengthBytes the maximum output of the process in bytes
 * @param timeoutMillis the timeout of the process
 * @throws RuntimeException if the process returns larger than [maxOutputLengthBytes] or takes longer than [timeoutMillis] to return [Credentials]
 */
public class ProcessCredentialsProvider(
    private val credentialProcess: String,
    private val platformProvider: PlatformProvider = Platform,
    private val maxOutputLengthBytes: Long = 64 * 1024,
    private val timeoutMillis: Long = 60_000,
) : CredentialsProvider {
    override suspend fun getCredentials(): Credentials {
        val logger = Logger.getLogger<ProcessCredentialsProvider>()

        val (exitCode, output) = try {
            executeCommand("\"$credentialProcess\"", platformProvider, maxOutputLengthBytes, timeoutMillis)
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
                resp.expiration ?: Instant.MAX_VALUE,
                PROVIDER_NAME,
            )
            else -> throw CredentialsProviderException("Credentials response was not of expected format")
        }
    }
}
