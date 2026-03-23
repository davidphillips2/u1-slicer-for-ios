package com.u1.slicer.shared.platform.file

/**
 * ZIP file entry interface
 */
interface ZipEntry {
    val name: String
    val size: Long
}

/**
 * ZIP file reader interface for extracting 3MF files (which are ZIP archives)
 */
interface ZipFileReader {
    /**
     * Get all entries in the ZIP file
     */
    suspend fun getEntries(path: String): List<ZipEntry>

    /**
     * Extract a single entry as text
     */
    suspend fun extractText(path: String, entryName: String): String

    /**
     * Extract a single entry as bytes
     */
    suspend fun extractBytes(path: String, entryName: String): ByteArray

    /**
     * Check if an entry exists
     */
    suspend fun containsEntry(path: String, entryName: String): Boolean
}

/**
 * Expected ZIP file reader factory
 */
expect class ZipFileReaderFactory() {
    fun createReader(): ZipFileReader
}
