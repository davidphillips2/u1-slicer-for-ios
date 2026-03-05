package com.u1.slicer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "slice_jobs")
data class SliceJob(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modelName: String,
    val gcodePath: String,
    val totalLayers: Int,
    val estimatedTimeSeconds: Float,
    val estimatedFilamentMm: Float,
    val layerHeight: Float,
    val fillDensity: Float,
    val nozzleTemp: Int,
    val bedTemp: Int,
    val supportEnabled: Boolean,
    val filamentType: String,
    val timestamp: Long = System.currentTimeMillis()
)
