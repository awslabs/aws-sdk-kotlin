import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.mergeSequential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(InternalSdkApi::class)
class FlowUtilTest {

    @Test
    fun testMergingFlows() = runTest {
        val a = flowOf(1)
        val b = flowOf(2, 3, 4)
        val merged = mergeSequential(a, b).toList()
        assertEquals(listOf(1, 2, 3, 4), merged)
    }

    @Test
    fun testMergingEmptyFlow() = runTest {
        val a: Flow<Int> = flowOf()
        val b: Flow<Int> = flowOf(4, 5, 6)

        val merged = mergeSequential(a, b).toList()
        assertEquals(listOf(4, 5, 6), merged)
    }

    @Test
    fun testMergingOneFlow() = runTest {
        val a = flowOf(1, 2, 3)
        val merged = mergeSequential(a).toList()

        assertEquals(listOf(1, 2, 3), merged)
    }

    @Test
    fun testMergingSameFlow() = runTest {
        val a = flowOf(1, 2, 3)
        val merged = mergeSequential(a, a).toList()
        assertEquals(listOf(1, 2, 3, 1, 2, 3), merged)
    }
}