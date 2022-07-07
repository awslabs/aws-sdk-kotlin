package aws.sdk.kotlin.s3.transfermanager.handler

import aws.sdk.kotlin.s3.transfermanager.data.TransferType
import java.util.BitSet

public interface PausedOperation {
    public val transferType: TransferType
    public val lastEndPoints: Map<String, BitSet> // each file/object key's last updated chunk number

    public suspend fun canResume(): Boolean
}
