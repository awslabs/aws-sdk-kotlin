package utils.concurrent

import java.io.ByteArrayOutputStream
import java.util.concurrent.locks.ReentrantLock

actual class Lock actual constructor() {
    private val lock = ReentrantLock()

    init {
        ByteArrayOutputStream().use {  }
    }

    actual fun lock() {
        lock.lock()
    }

    actual fun unlock() {
        lock.unlock()
    }
}