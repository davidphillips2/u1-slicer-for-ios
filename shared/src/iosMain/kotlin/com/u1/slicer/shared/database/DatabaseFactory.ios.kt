package com.u1.slicer.shared.database

import app.cash.sqldelight.driver.native.wrapConnection
import com.u1.slicer.shared.data.FilamentProfile
import com.u1.slicer.shared.data.SliceJob
import com.u1.slicer.shared.database.U1SlicerDatabase

actual class DatabaseFactory private constructor() {

    private var driver: app.cash.sqldelight.driver.native.NativeSqliteDriver? = null
    private var database: IosU1SlicerDatabase? = null

    actual suspend fun createDatabase(): AppDatabase {
        val sqlDriver = app.cash.sqldelight.driver.native.NativeSqliteDriver(
            schema = U1SlicerDatabase.Schema,
            name = "u1_slicer.db"
        )
        driver = sqlDriver
        val sqlDelightDb = U1SlicerDatabase(sqlDriver)
        val db = IosU1SlicerDatabase(sqlDelightDb)
        database = db
        return db
    }

    actual suspend fun close() {
        database = null
        driver?.close()
        driver = null
    }

    companion object {
        fun create(): DatabaseFactory = DatabaseFactory()
    }
}

/**
 * iOS implementation using SQLDelight
 */
class IosU1SlicerDatabase(private val sqlDriver: U1SlicerDatabase) : AppDatabase {

    override suspend fun getAllFilaments(): List<FilamentProfile> =
        sqlDriver.filamentQueriesQueries.selectAll().executeAsList().map { it.toFilamentProfile() }

    override suspend fun getFilamentById(id: Long): FilamentProfile? =
        sqlDriver.filamentQueriesQueries.getById(id).executeAsOneOrNull()?.toFilamentProfile()

    override suspend fun insertFilament(filament: FilamentProfile): Long {
        sqlDriver.filamentQueriesQueries.insert(
            name = filament.name,
            material = filament.material,
            nozzle_temp = filament.nozzleTemp.toLong(),
            bed_temp = filament.bedTemp.toLong(),
            print_speed = filament.printSpeed.toDouble(),
            retract_length = filament.retractLength.toDouble(),
            retract_speed = filament.retractSpeed.toDouble(),
            color = filament.color,
            density = filament.density.toDouble(),
            is_default = if (filament.isDefault) 1L else 0L
        )
        return sqlDriver.filamentQueriesQueries.selectLastInsertRowId().executeAsOne()
    }

    override suspend fun updateFilament(filament: FilamentProfile) {
        sqlDriver.filamentQueriesQueries.update(
            name = filament.name,
            material = filament.material,
            nozzle_temp = filament.nozzleTemp.toLong(),
            bed_temp = filament.bedTemp.toLong(),
            print_speed = filament.printSpeed.toDouble(),
            retract_length = filament.retractLength.toDouble(),
            retract_speed = filament.retractSpeed.toDouble(),
            color = filament.color,
            density = filament.density.toDouble(),
            is_default = if (filament.isDefault) 1L else 0L,
            id = filament.id
        )
    }

    override suspend fun deleteFilament(filament: FilamentProfile) {
        sqlDriver.filamentQueriesQueries.deleteById(filament.id)
    }

    override suspend fun getDefaultFilament(): FilamentProfile? =
        sqlDriver.filamentQueriesQueries.selectDefault().executeAsOneOrNull()?.toFilamentProfile()

    override suspend fun getAllSliceJobs(): List<SliceJob> =
        sqlDriver.sliceJobQueriesQueries.selectAll().executeAsList().map { it.toSliceJob() }

    override suspend fun getSliceJobById(id: Long): SliceJob? =
        sqlDriver.sliceJobQueriesQueries.getById(id).executeAsOneOrNull()?.toSliceJob()

    override suspend fun insertSliceJob(job: SliceJob): Long {
        sqlDriver.sliceJobQueriesQueries.insert(
            model_name = job.modelName,
            gcode_path = job.gcodePath,
            total_layers = job.totalLayers.toLong(),
            estimated_time_seconds = job.estimatedTimeSeconds.toDouble(),
            estimated_filament_mm = job.estimatedFilamentMm.toDouble(),
            layer_height = job.layerHeight.toDouble(),
            fill_density = job.fillDensity.toDouble(),
            nozzle_temp = job.nozzleTemp.toLong(),
            bed_temp = job.bedTemp.toLong(),
            support_enabled = if (job.supportEnabled) 1L else 0L,
            filament_type = job.filamentType,
            timestamp = job.timestamp
        )
        return sqlDriver.sliceJobQueriesQueries.selectLastInsertRowId().executeAsOne()
    }

    override suspend fun deleteSliceJob(job: SliceJob) {
        sqlDriver.sliceJobQueriesQueries.deleteById(job.id)
    }

    override suspend fun clearOldJobs(timestampThreshold: Long) {
        sqlDriver.sliceJobQueriesQueries.deleteOlderThan(timestampThreshold)
    }

    override suspend fun clearAllData() {
        sqlDriver.filamentQueriesQueries.deleteAll()
        sqlDriver.sliceJobQueriesQueries.deleteAll()
    }
}

// Extension functions to convert SQLDelight results to shared data classes
private fun Filament_profiles.toFilamentProfile() = FilamentProfile(
    id = id,
    name = name,
    material = material,
    nozzleTemp = nozzle_temp.toInt(),
    bedTemp = bed_temp.toInt(),
    printSpeed = print_speed.toFloat(),
    retractLength = retract_length.toFloat(),
    retractSpeed = retract_speed.toFloat(),
    color = color,
    density = density.toFloat(),
    isDefault = is_default == 1L
)

private fun Slice_jobs.toSliceJob() = SliceJob(
    id = id,
    modelName = model_name,
    gcodePath = gcode_path,
    totalLayers = total_layers.toInt(),
    estimatedTimeSeconds = estimated_time_seconds.toFloat(),
    estimatedFilamentMm = estimated_filament_mm.toFloat(),
    layerHeight = layer_height.toFloat(),
    fillDensity = fill_density.toFloat(),
    nozzleTemp = nozzle_temp.toInt(),
    bedTemp = bed_temp.toInt(),
    supportEnabled = support_enabled == 1L,
    filamentType = filament_type ?: "",
    timestamp = timestamp
)
