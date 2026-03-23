package com.u1.slicer.shared.platform.file

import android.content.Context
import java.io.File

actual class FileReaderFactory(private val context: Context) {
    actual fun createReader(): FileReader = AndroidFileReader(context)
    actual fun createFilePath(path: String): FilePath = AndroidFilePath(path)
}

class AndroidFileReader(private val context: Context) : FileReader {
    override suspend fun readText(path: String): String {
        return File(path).readText()
    }

    override suspend fun readBytes(path: String): ByteArray {
        return File(path).readBytes()
    }

    override suspend fun exists(path: String): Boolean {
        return File(path).exists()
    }

    override suspend fun getFileSize(path: String): Long {
        return File(path).length()
    }
}

class AndroidFilePath(override val path: String) : FilePath {
    override val name: String
        get() = File(path).name
    override val extension: String
        get() = File(path).extension.substringAfterLast('.', "")
}
