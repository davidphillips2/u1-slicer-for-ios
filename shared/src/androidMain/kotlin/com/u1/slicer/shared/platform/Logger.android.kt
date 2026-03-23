package com.u1.slicer.shared.platform

import android.util.Log

actual fun getLogger(): Logger = AndroidLogger()

class AndroidLogger : Logger {
    override fun d(tag: String, message: String) { Log.d(tag, message) }
    override fun w(tag: String, message: String) { Log.w(tag, message) }
    override fun e(tag: String, message: String) { Log.e(tag, message) }
    override fun i(tag: String, message: String) { Log.i(tag, message) }
}
