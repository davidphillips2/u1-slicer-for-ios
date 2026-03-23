package com.u1.slicer.shared.database.shared

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.u1.slicer.shared.database.FilamentQueriesQueries
import com.u1.slicer.shared.database.SliceJobQueriesQueries
import com.u1.slicer.shared.database.U1SlicerDatabase
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<U1SlicerDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = U1SlicerDatabaseImpl.Schema

internal fun KClass<U1SlicerDatabase>.newInstance(driver: SqlDriver): U1SlicerDatabase =
    U1SlicerDatabaseImpl(driver)

private class U1SlicerDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), U1SlicerDatabase {
  override val filamentQueriesQueries: FilamentQueriesQueries = FilamentQueriesQueries(driver)

  override val sliceJobQueriesQueries: SliceJobQueriesQueries = SliceJobQueriesQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE filament_profiles (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT,
          |    name TEXT NOT NULL,
          |    material TEXT NOT NULL,        -- PLA, PETG, ABS, TPU, ASA, PA, PVA
          |    nozzle_temp INTEGER NOT NULL,
          |    bed_temp INTEGER NOT NULL,
          |    print_speed REAL NOT NULL,
          |    retract_length REAL NOT NULL,
          |    retract_speed REAL NOT NULL,
          |    color TEXT DEFAULT '#808080',  -- Hex color for UI display
          |    density REAL DEFAULT 1.24,     -- g/cm3 for weight estimation
          |    is_default INTEGER DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE slice_jobs (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT,
          |    model_name TEXT NOT NULL,
          |    gcode_path TEXT NOT NULL,
          |    total_layers INTEGER NOT NULL,
          |    estimated_time_seconds REAL NOT NULL,
          |    estimated_filament_mm REAL NOT NULL,
          |    layer_height REAL NOT NULL,
          |    fill_density REAL NOT NULL,
          |    nozzle_temp INTEGER NOT NULL,
          |    bed_temp INTEGER NOT NULL,
          |    support_enabled INTEGER NOT NULL,
          |    filament_type TEXT NOT NULL,
          |    timestamp INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
