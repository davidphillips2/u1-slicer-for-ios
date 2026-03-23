package com.u1.slicer.shared.platform.file

import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.stringWithDataEncoding
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.UTF8StringEncoding

actual class FileReaderFactory {
    actual fun createReader(): FileReader = IosFileReader()
    actual fun createFilePath(path: String): FilePath = IosFilePath(path)
}

class IosFileReader : FileReader {
    override suspend fun readText(path: String): String {
        val data = NSData.dataWithContentsOfFile(path) ?: throw IOException("File not found: $path")
        return NSString.stringWithData(data, UTF8StringEncoding) as String
    }

    override suspend fun readBytes(path: String): ByteArray {
        val data = NSData.dataWithContentsOfFile(path) ?: throw IOException("File not found: $path")
        return data.toByteArray()
    }

    override suspend fun exists(path: String): Boolean {
        val data = NSData.dataWithContentsOfFile(path)
        return data != null
    }

    override suspend fun getFileSize(path: String): Long {
        val data = NSData.dataWithContentsOfFile(path) ?: throw IOException("File not found: $path")
        return data.length.toLong()
    }
}

class IosFilePath(override val path: String) : FilePath {
    override val name: String
        get() = path.substringAfterLast('/')
    override val extension: String
        get() = path.substringAfterLast('.', "").substringAfterLast('/')
}

// Helper extension to convert NSData to ByteArray
fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(this.length.toInt())
    this.getBytes(bytes.toByteArrayRef(), this.length)
    return bytes
}

// IOException for iOS
class IOException(message: String) : Exception(message)
