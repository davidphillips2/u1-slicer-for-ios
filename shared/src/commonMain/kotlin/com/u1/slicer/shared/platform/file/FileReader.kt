package com.u1.slicer.shared.platform.file

/**
 * Platform-agnostic file reader interface for KMP
 */
interface FileReader {
    /**
     * Read all text content from a file at the given path
     */
    suspend fun readText(path: String): String

    /**
     * Read all binary content from a file at the given path
     */
    suspend fun readBytes(path: String): ByteArray

    /**
     * Check if a file exists at the given path
     */
    suspend fun exists(path: String): Boolean

    /**
     * Get the file size in bytes
     */
    suspend fun getFileSize(path: String): Long
}

/**
 * Platform-agnostic file path interface
 */
interface FilePath {
    val path: String
    val name: String
    val extension: String
}

/**
 * Expected file reader factory
 * Note: Android implementation requires Context parameter
 */
expect class FileReaderFactory {
    fun createReader(): FileReader
    fun createFilePath(path: String): FilePath
}
