package com.u1.slicer.shared.platform

/**
 * Platform-agnostic logging interface
 */
interface Logger {
    fun d(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String)
    fun i(tag: String, message: String)
}

/**
 * Expected logger factory
 */
expect fun getLogger(): Logger

// Default console logger for common code
class ConsoleLogger : Logger {
    override fun d(tag: String, message: String) = println("D/$tag: $message")
    override fun w(tag: String, message: String) = println("W/$tag: $message")
    override fun e(tag: String, message: String) = println("E/$tag: $message")
    override fun i(tag: String, message: String) = println("I/$tag: $message")
}
