package com.u1.slicer.shared.platform

import platform.Foundation.NSLog

actual fun getLogger(): Logger = IosLogger()

class IosLogger : Logger {
    override fun d(tag: String, message: String) = NSLog("D/$tag: $message")
    override fun w(tag: String, message: String) = NSLog("W/$tag: $message")
    override fun e(tag: String, message: String) = NSLog("E/$tag: $message")
    override fun i(tag: String, message: String) = NSLog("I/$tag: $message")
}
