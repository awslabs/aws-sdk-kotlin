package aws.sdk.kotlin.services.sqs.internal

/**
 * Controls when MD5 checksum validation is performed for SQS operations.
 *
 * This configuration determines under which conditions checksums will be automatically
 * calculated and validated for SQS message operations.
 *
 * Valid values:
 * - ALWAYS - Validates checksums for both sending and receiving operations
 *           (SendMessage, SendMessageBatch, and ReceiveMessage)
 * - WHEN_SENDING - Validates checksums only when sending messages
 *                 (SendMessage and SendMessageBatch)
 * - WHEN_RECEIVING - Validates checksums only when receiving messages
 *                   (ReceiveMessage)
 * - NEVER - Disables checksum validation completely
 *
 * Default: ALWAYS
 */
public enum class ValidationEnabled {
    ALWAYS,
    WHEN_SENDING,
    WHEN_RECEIVING,
    NEVER
}

/**
 * Specifies which parts of an SQS message should undergo MD5 checksum validation.
 *
 * This configuration determines which components of a message will be validated
 * when checksum validation is enabled.
 *
 * Valid values:
 * - MESSAGE_ATTRIBUTES - Validates checksums for message attributes
 * - MESSAGE_SYSTEM_ATTRIBUTES - Validates checksums for message system attributes
 *   (Note: Not available for ReceiveMessage operations as SQS does not calculate
 *   checksums for system attributes during message receipt)
 * - MESSAGE_BODY - Validates checksums for the message body
 *
 * Default: All scopes enabled
 */
public enum class ValidationScope {
    MESSAGE_ATTRIBUTES,
    MESSAGE_SYSTEM_ATTRIBUTES,
    MESSAGE_BODY
}
