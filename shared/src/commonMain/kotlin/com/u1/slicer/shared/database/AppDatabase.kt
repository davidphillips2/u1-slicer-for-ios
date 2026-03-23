package com.u1.slicer.shared.database

import com.u1.slicer.shared.data.FilamentProfile
import com.u1.slicer.shared.data.SliceJob

/**
 * Platform-agnostic database interface for filament profiles and slice job history
 */
interface AppDatabase {
    // Filament profiles
    suspend fun getAllFilaments(): List<FilamentProfile>
    suspend fun getFilamentById(id: Long): FilamentProfile?
    suspend fun insertFilament(filament: FilamentProfile): Long
    suspend fun updateFilament(filament: FilamentProfile)
    suspend fun deleteFilament(filament: FilamentProfile)
    suspend fun getDefaultFilament(): FilamentProfile?

    // Slice jobs
    suspend fun getAllSliceJobs(): List<SliceJob>
    suspend fun getSliceJobById(id: Long): SliceJob?
    suspend fun insertSliceJob(job: SliceJob): Long
    suspend fun deleteSliceJob(job: SliceJob)
    suspend fun clearOldJobs(timestampThreshold: Long)

    // Database management
    suspend fun clearAllData()
}
