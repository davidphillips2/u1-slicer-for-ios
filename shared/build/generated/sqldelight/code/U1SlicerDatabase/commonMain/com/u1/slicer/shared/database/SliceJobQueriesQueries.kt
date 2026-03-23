package com.u1.slicer.shared.database

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Double
import kotlin.Long
import kotlin.String

public class SliceJobQueriesQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAll(mapper: (
    id: Long,
    model_name: String,
    gcode_path: String,
    total_layers: Long,
    estimated_time_seconds: Double,
    estimated_filament_mm: Double,
    layer_height: Double,
    fill_density: Double,
    nozzle_temp: Long,
    bed_temp: Long,
    support_enabled: Long,
    filament_type: String,
    timestamp: Long,
  ) -> T): Query<T> = Query(976_507_227, arrayOf("slice_jobs"), driver, "SliceJobQueries.sq",
      "selectAll", """
  |SELECT slice_jobs.id, slice_jobs.model_name, slice_jobs.gcode_path, slice_jobs.total_layers, slice_jobs.estimated_time_seconds, slice_jobs.estimated_filament_mm, slice_jobs.layer_height, slice_jobs.fill_density, slice_jobs.nozzle_temp, slice_jobs.bed_temp, slice_jobs.support_enabled, slice_jobs.filament_type, slice_jobs.timestamp FROM slice_jobs
  |ORDER BY timestamp DESC
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getDouble(4)!!,
      cursor.getDouble(5)!!,
      cursor.getDouble(6)!!,
      cursor.getDouble(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getString(11)!!,
      cursor.getLong(12)!!
    )
  }

  public fun selectAll(): Query<Slice_jobs> = selectAll { id, model_name, gcode_path, total_layers,
      estimated_time_seconds, estimated_filament_mm, layer_height, fill_density, nozzle_temp,
      bed_temp, support_enabled, filament_type, timestamp ->
    Slice_jobs(
      id,
      model_name,
      gcode_path,
      total_layers,
      estimated_time_seconds,
      estimated_filament_mm,
      layer_height,
      fill_density,
      nozzle_temp,
      bed_temp,
      support_enabled,
      filament_type,
      timestamp
    )
  }

  public fun <T : Any> getById(id: Long, mapper: (
    id: Long,
    model_name: String,
    gcode_path: String,
    total_layers: Long,
    estimated_time_seconds: Double,
    estimated_filament_mm: Double,
    layer_height: Double,
    fill_density: Double,
    nozzle_temp: Long,
    bed_temp: Long,
    support_enabled: Long,
    filament_type: String,
    timestamp: Long,
  ) -> T): Query<T> = GetByIdQuery(id) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getDouble(4)!!,
      cursor.getDouble(5)!!,
      cursor.getDouble(6)!!,
      cursor.getDouble(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getString(11)!!,
      cursor.getLong(12)!!
    )
  }

  public fun getById(id: Long): Query<Slice_jobs> = getById(id) { id_, model_name, gcode_path,
      total_layers, estimated_time_seconds, estimated_filament_mm, layer_height, fill_density,
      nozzle_temp, bed_temp, support_enabled, filament_type, timestamp ->
    Slice_jobs(
      id_,
      model_name,
      gcode_path,
      total_layers,
      estimated_time_seconds,
      estimated_filament_mm,
      layer_height,
      fill_density,
      nozzle_temp,
      bed_temp,
      support_enabled,
      filament_type,
      timestamp
    )
  }

  public fun selectLastInsertRowId(): ExecutableQuery<Long> = Query(1_348_114_848, driver,
      "SliceJobQueries.sq", "selectLastInsertRowId", "SELECT last_insert_rowid() AS rowid") {
      cursor ->
    cursor.getLong(0)!!
  }

  public fun insert(
    model_name: String,
    gcode_path: String,
    total_layers: Long,
    estimated_time_seconds: Double,
    estimated_filament_mm: Double,
    layer_height: Double,
    fill_density: Double,
    nozzle_temp: Long,
    bed_temp: Long,
    support_enabled: Long,
    filament_type: String,
    timestamp: Long,
  ) {
    driver.execute(-488_514_525, """
        |INSERT INTO slice_jobs (
        |    model_name, gcode_path, total_layers, estimated_time_seconds,
        |    estimated_filament_mm, layer_height, fill_density,
        |    nozzle_temp, bed_temp, support_enabled, filament_type, timestamp
        |)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 12) {
          bindString(0, model_name)
          bindString(1, gcode_path)
          bindLong(2, total_layers)
          bindDouble(3, estimated_time_seconds)
          bindDouble(4, estimated_filament_mm)
          bindDouble(5, layer_height)
          bindDouble(6, fill_density)
          bindLong(7, nozzle_temp)
          bindLong(8, bed_temp)
          bindLong(9, support_enabled)
          bindString(10, filament_type)
          bindLong(11, timestamp)
        }
    notifyQueries(-488_514_525) { emit ->
      emit("slice_jobs")
    }
  }

  public fun deleteById(id: Long) {
    driver.execute(-667_427_705, """
        |DELETE FROM slice_jobs
        |WHERE id = ?
        """.trimMargin(), 1) {
          bindLong(0, id)
        }
    notifyQueries(-667_427_705) { emit ->
      emit("slice_jobs")
    }
  }

  public fun deleteOlderThan(timestamp: Long) {
    driver.execute(-534_409_920, """
        |DELETE FROM slice_jobs
        |WHERE timestamp < ?
        """.trimMargin(), 1) {
          bindLong(0, timestamp)
        }
    notifyQueries(-534_409_920) { emit ->
      emit("slice_jobs")
    }
  }

  public fun deleteAll() {
    driver.execute(-1_961_193_908, """DELETE FROM slice_jobs""", 0)
    notifyQueries(-1_961_193_908) { emit ->
      emit("slice_jobs")
    }
  }

  private inner class GetByIdQuery<out T : Any>(
    public val id: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("slice_jobs", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("slice_jobs", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(3_135_518, """
    |SELECT slice_jobs.id, slice_jobs.model_name, slice_jobs.gcode_path, slice_jobs.total_layers, slice_jobs.estimated_time_seconds, slice_jobs.estimated_filament_mm, slice_jobs.layer_height, slice_jobs.fill_density, slice_jobs.nozzle_temp, slice_jobs.bed_temp, slice_jobs.support_enabled, slice_jobs.filament_type, slice_jobs.timestamp FROM slice_jobs
    |WHERE id = ?
    """.trimMargin(), mapper, 1) {
      bindLong(0, id)
    }

    override fun toString(): String = "SliceJobQueries.sq:getById"
  }
}
