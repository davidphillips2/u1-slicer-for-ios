package com.u1.slicer.shared.platform.file

import com.u1.slicer.shared.platform.IOException

/**
 * iOS implementation of ZIP file reader.
 * TODO: Requires ZIPFoundation or similar library for full implementation.
 */
actual class ZipFileReaderFactory {
    actual fun createReader(): ZipFileReader = IosZipFileReader()
}

class IosZipFileReader : ZipFileReader {
    override suspend fun getEntries(path: String): List<ZipEntry> {
        // TODO: Implement ZIP parsing for iOS
        // Would require ZIPFoundation or NSZipArchive
        return listOf()
    }

    override suspend fun extractText(path: String, entryName: String): String {
        throw IOException("ZIP extraction not yet implemented for iOS")
    }

    override suspend fun extractBytes(path: String, entryName: String): ByteArray {
        throw IOException("ZIP extraction not yet implemented for iOS")
    }

    override suspend fun containsEntry(path: String, entryName: String): Boolean {
        // TODO: Implement ZIP parsing for iOS
        return false
    }
}
