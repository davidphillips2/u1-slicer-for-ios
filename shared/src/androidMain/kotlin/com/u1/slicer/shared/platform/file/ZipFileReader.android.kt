package com.u1.slicer.shared.platform.file

import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

actual class ZipFileReaderFactory {
    actual fun createReader(): ZipFileReader = AndroidZipFileReader()
}

class AndroidZipFileReader : ZipFileReader {
    override suspend fun getEntries(path: String): List<ZipEntry> {
        val zip = ZipFile(path)
        return zip.entries().toList().map { entry ->
            AndroidZipEntry(entry.name, entry.size)
        }
    }

    override suspend fun extractText(path: String, entryName: String): String {
        ZipFile(path).use { zip ->
            val entry = zip.getEntry(entryName) ?: throw IOException("Entry not found: $entryName")
            return zip.getInputStream(entry).bufferedReader().use { it.readText() }
        }
    }

    override suspend fun extractBytes(path: String, entryName: String): ByteArray {
        ZipFile(path).use { zip ->
            val entry = zip.getEntry(entryName) ?: throw IOException("Entry not found: $entryName")
            return zip.getInputStream(entry).readBytes()
        }
    }

    override suspend fun containsEntry(path: String, entryName: String): Boolean {
        ZipFile(path).use { zip ->
            return zip.getEntry(entryName) != null
        }
    }
}

data class AndroidZipEntry(
    override val name: String,
    override val size: Long
) : ZipEntry
