package com.u1.slicer.shared.platform

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.timeval

actual object PlatformUtils {
    @OptIn(ExperimentalForeignApi::class)
    actual fun currentTimeMillis(): Long {
        memScoped {
            val timeVal = alloc<timeval>()
            platform.posix.gettimeofday(timeVal.ptr, null)
            return timeVal.tv_sec * 1000L + timeVal.tv_usec / 1000L
        }
    }

    actual fun formatFloat(format: String, vararg args: Any): String {
        // Simple formatting for common cases
        var result = format
        var argIndex = 0

        // Process format specifiers in order
        var startIndex = 0
        while (argIndex < args.size) {
            val specIndex = result.indexOf("%", startIndex)
            if (specIndex == -1) break

            val nextIndex = specIndex + 1
            if (nextIndex >= result.length) break

            val spec = when {
                result.length > nextIndex + 2 && result[nextIndex + 1] == '.' && result[nextIndex + 2].isDigit() -> {
                    result.substring(specIndex, nextIndex + 3)
                }
                result[nextIndex].isDigit() -> {
                    result.substring(specIndex, nextIndex + 1)
                }
                else -> {
                    result.substring(specIndex, nextIndex + 1)
                }
            }

            val value = args[argIndex]
            val replacement = when (spec) {
                "%.1f" -> NSString.stringWithFormat("%.1f", (value as Number).toDouble()).toString()
                "%.0f" -> NSString.stringWithFormat("%.0f", (value as Number).toDouble()).toString()
                "%d" -> NSString.stringWithFormat("%d", (value as Number).toInt()).toString()
                "%f" -> NSString.stringWithFormat("%f", (value as Number).toDouble()).toString()
                else -> value.toString()
            }

            result = result.substring(0, specIndex) + replacement + result.substring(specIndex + spec.length)
            startIndex = specIndex + replacement.length
            argIndex++
        }

        return result
    }
}
