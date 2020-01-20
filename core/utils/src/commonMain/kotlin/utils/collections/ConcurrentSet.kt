package utils.collections

import utils.concurrent.Lock

// Original code from https://github.com/ktorio/ktor/blob/3ee2ae68734ab6d3740ec5ba8e978c2b5502e72d/ktor-utils/common/src/io/ktor/util/collections/ConcurrentSet.kt
class ConcurrentSet<K> internal constructor(delegate: MutableSet<K>, lock: Lock) :
    ConcurrentCollection<K>(delegate, lock), MutableSet<K>