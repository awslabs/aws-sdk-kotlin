package software.aws.kotlin.utils.collections

import software.aws.kotlin.utils.concurrent.Lock
import software.aws.kotlin.utils.concurrent.withLock

// Original code from https://github.com/ktorio/ktor/blob/3ee2ae68734ab6d3740ec5ba8e978c2b5502e72d/ktor-utils/common/src/io/ktor/util/collections/ConcurrentCollection.kt
open class ConcurrentCollection<E> internal constructor(
    private val delegate: MutableCollection<E>,
    private val lock: Lock
) : MutableCollection<E> {
    override val size: Int get() = lock.withLock {
        delegate.size
    }

    override fun contains(element: E): Boolean = lock.withLock {
        delegate.contains(element)
    }

    override fun containsAll(elements: Collection<E>): Boolean = lock.withLock {
        delegate.containsAll(elements)
    }

    override fun isEmpty(): Boolean = lock.withLock {
        delegate.isEmpty()
    }

    override fun add(element: E): Boolean = lock.withLock {
        delegate.add(element)
    }

    override fun addAll(elements: Collection<E>): Boolean = lock.withLock {
        delegate.addAll(elements)
    }

    override fun clear() = lock.withLock {
        delegate.clear()
    }

    override fun iterator(): MutableIterator<E> = delegate.iterator()

    override fun remove(element: E): Boolean = lock.withLock {
        delegate.remove(element)
    }

    override fun removeAll(elements: Collection<E>): Boolean = lock.withLock {
        delegate.removeAll(elements)
    }

    override fun retainAll(elements: Collection<E>): Boolean = lock.withLock {
        delegate.retainAll(elements)
    }
}
