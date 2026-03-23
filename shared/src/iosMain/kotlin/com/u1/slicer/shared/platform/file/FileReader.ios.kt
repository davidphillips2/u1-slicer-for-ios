package com.u1.slicer.shared.platform.file

import com.u1.slicer.shared.platform.IOException
import kotlinx.cinterop.*
import platform.Foundation.*

actual class FileReaderFactory {
    actual fun createReader(): FileReader = IosFileReader()
    actual fun createFilePath(path: String): FilePath = IosFilePath(path)
}

@OptIn(ExperimentalForeignApi::class)
class IosFileReader : FileReader {
    override suspend fun readText(path: String): String {
        val nsString = NSString.stringWithString(path)
        val nsUrl = NSURL.fileURLWithPath(nsString)
        val data = NSData.dataWithContentsOfURL(nsUrl)
            ?: throw IOException("File not found: $path")

        val nsStringResult = NSString.create(data, NSUTF8StringEncoding) as? NSString
            ?: throw IOException("Failed to decode file as UTF-8")
        return nsStringResult as String
    }

    override suspend fun readBytes(path: String): ByteArray {
        val nsString = NSString.stringWithString(path)
        val nsUrl = NSURL.fileURLWithPath(nsString)
        val data = NSData.dataWithContentsOfURL(nsUrl)
            ?: throw IOException("File not found: $path")

        val length = data.length.toInt()
        val bytes = ByteArray(length)

        // Copy NSData to ByteArray
        memScoped {
            val buffer = allocArray<ByteVar>(length)
            data.getBytes(buffer, length.toULong())
            for (i in 0 until length) {
                bytes[i] = buffer[i]
            }
        }
        return bytes
    }

    override suspend fun exists(path: String): Boolean {
        val nsString = NSString.stringWithString(path)
        val nsUrl = NSURL.fileURLWithPath(nsString)
        val data = NSData.dataWithContentsOfURL(nsUrl)
        return data != null
    }

    override suspend fun getFileSize(path: String): Long {
        val nsString = NSString.stringWithString(path)
        val nsUrl = NSURL.fileURLWithPath(nsString)
        val data = NSData.dataWithContentsOfURL(nsUrl)
            ?: return 0L
        return data.length.toLong()
    }
}

class IosFilePath(override val path: String) : FilePath {
    override val name: String
        get() = path.substringAfterLast("/")

    override val extension: String
        get() = path.substringAfterLast('.', "")
}
