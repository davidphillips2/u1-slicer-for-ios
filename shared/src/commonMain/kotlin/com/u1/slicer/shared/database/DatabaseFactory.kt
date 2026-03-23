package com.u1.slicer.shared.database

/**
 * Platform-agnostic database factory
 */
expect class DatabaseFactory {
    /**
     * Create and initialize the database
     */
    suspend fun createDatabase(): AppDatabase

    /**
     * Close any open database connections
     */
    suspend fun close()
}
