package software.aws.kotlin.utils.collections

import software.aws.kotlin.utils.concurrent.Lock
import software.aws.kotlin.utils.concurrent.withLock
import kotlin.collections.MutableMap.MutableEntry

// Original code from https://github.com/ktorio/ktor/blob/3ee2ae68734ab6d3740ec5ba8e978c2b5502e72d/ktor-utils/common/src/io/ktor/util/collections/ConcurrentMap.kt
class ConcurrentMap<K, V> : MutableMap<K, V> {
    private val lock = Lock()
    private val delegate = mutableMapOf<K, V>()

    override val size: Int = lock.withLock {
        delegate.size
    }

    override fun containsKey(key: K): Boolean = lock.withLock {
        delegate.containsKey(key)
    }

    override fun containsValue(value: V): Boolean = lock.withLock {
        delegate.containsValue(value)
    }

    override fun get(key: K): V? = lock.withLock {
        delegate[key]
    }

    override fun isEmpty(): Boolean = lock.withLock {
        delegate.isEmpty()
    }

    override val entries: MutableSet<MutableEntry<K, V>> = ConcurrentSet(delegate.entries, lock)

    override val keys: MutableSet<K> = ConcurrentSet(delegate.keys, lock)

    override val values: MutableCollection<V> = ConcurrentCollection(delegate.values, lock)

    override fun clear() = lock.withLock {
        delegate.clear()
    }

    override fun put(key: K, value: V): V? = lock.withLock {
        delegate.put(key, value)
    }

    override fun putAll(from: Map<out K, V>) = lock.withLock {
        delegate.putAll(from)
    }

    override fun remove(key: K): V? = lock.withLock {
        delegate.remove(key)
    }

    fun computeIfAbsent(key: K, block: () -> V): V = lock.withLock {
        get(key)?.let { return it }

        val result = block()
        put(key, result)
        return result
    }
}