package com.u1.slicer.shared.platform.file

import platform.Foundation.*
import platform.Foundation.NSZipArchive

actual class ZipFileReaderFactory {
    actual fun createReader(): ZipFileReader = IosZipFileReader()
}

class IosZipFileReader : ZipFileReader {
    override suspend fun getEntries(path: String): List<ZipEntry> {
        val nsString = NSString.stringWithString(path)
        val nsUrl = NSURL.fileURLWithPath(nsString)
        val error = mutableListOf<NSError?>()

        // Use NSFileCoordinator for coordinated file access
        var entries = emptyList<ZipEntry>()
        NSFileCoordinator.coordinateReadingItemAtURL(
            itemURL = nsUrl,
            options = NSFileCoordinatorReadingOptionsWithoutChanges,
            error = null
        ) { url in
            val zipData = NSData.dataWithContentsOfURL(url)
            if (zipData != null) {
                // For now, return empty list - full ZIP parsing would require more complex implementation
                entries = listOf()
            }
        }

        return entries
    }

    override suspend fun extractText(path: String, entryName: String): String {
        val data = extractEntryData(path, entryName)
        return NSString.stringWithData(data, NSUTF8StringEncoding) as String
    }

    override suspend fun extractBytes(path: String, entryName: String): ByteArray {
        return extractEntryData(path, entryName).toByteArray()
    }

    override suspend fun containsEntry(path: String, entryName: String): Boolean {
        // Simplified implementation - would need full ZIP parsing for accuracy
        return true
    }

    private fun extractEntryData(path: String, entryName: String): NSData {
        val nsString = NSString.stringWithString(path)
        val nsUrl = NSURL.fileURLWithPath(nsString)

        var result: NSData? = null
        NSFileCoordinator.coordinateReadingItemAtURL(
            itemURL = nsUrl,
            options = NSFileCoordinatorReadingOptionsWithoutChanges,
            error = null
        ) { url ->
            // Extract specific entry from ZIP
            // Full implementation would use a ZIP library or more detailed parsing
            result = NSData.dataWithContentsOfURL(url)
        }

        return result ?: throw IOException("Failed to extract entry: $entryName")
    }
}

// Helper extension
fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(this.length.toInt())
    this.getBytes(bytes.toByteArrayRef(), this.length)
    return bytes
}
