package aws.sdk.kotlin.s3.transfermanager.handler

import aws.sdk.kotlin.s3.transfermanager.data.TransferType
import java.util.BitSet

interface PausedOperation {
    val transferType: TransferType
    val lastEndPoints: Map<String, BitSet> // each file/object key's last updated chunk number

    suspend fun canResume(): Boolean
}
