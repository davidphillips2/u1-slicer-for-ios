package com.u1.slicer.shared.di

import com.u1.slicer.shared.database.AppDatabase
import com.u1.slicer.shared.database.DatabaseFactory

/**
 * Dependency injection container for shared code
 */
class SharedAppContainer(
    private val databaseFactory: DatabaseFactory
) {
    private var database: AppDatabase? = null

    /**
     * Get or create the database instance
     */
    suspend fun getDatabase(): AppDatabase {
        if (database == null) {
            database = databaseFactory.createDatabase()
        }
        return database!!
    }

    /**
     * Close all resources
     */
    suspend fun close() {
        database?.let { databaseFactory.close() }
        database = null
    }
}
