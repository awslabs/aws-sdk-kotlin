package aws.sdk.kotlin.runtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

// FIXME relocate to smithy-kotlin
@InternalSdkApi
public fun <T> mergeSequential(vararg flows: Flow<T>): Flow<T> = flow {
    flows.forEach { flow -> emitAll(flow) }
}
