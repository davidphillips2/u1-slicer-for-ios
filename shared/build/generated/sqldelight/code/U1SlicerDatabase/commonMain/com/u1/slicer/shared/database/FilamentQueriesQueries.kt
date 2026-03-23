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

public class FilamentQueriesQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAll(mapper: (
    id: Long,
    name: String,
    material: String,
    nozzle_temp: Long,
    bed_temp: Long,
    print_speed: Double,
    retract_length: Double,
    retract_speed: Double,
    color: String?,
    density: Double?,
    is_default: Long?,
  ) -> T): Query<T> = Query(1_616_005_094, arrayOf("filament_profiles"), driver,
      "FilamentQueries.sq", "selectAll", """
  |SELECT filament_profiles.id, filament_profiles.name, filament_profiles.material, filament_profiles.nozzle_temp, filament_profiles.bed_temp, filament_profiles.print_speed, filament_profiles.retract_length, filament_profiles.retract_speed, filament_profiles.color, filament_profiles.density, filament_profiles.is_default FROM filament_profiles
  |ORDER BY is_default DESC, name ASC
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getDouble(5)!!,
      cursor.getDouble(6)!!,
      cursor.getDouble(7)!!,
      cursor.getString(8),
      cursor.getDouble(9),
      cursor.getLong(10)
    )
  }

  public fun selectAll(): Query<Filament_profiles> = selectAll { id, name, material, nozzle_temp,
      bed_temp, print_speed, retract_length, retract_speed, color, density, is_default ->
    Filament_profiles(
      id,
      name,
      material,
      nozzle_temp,
      bed_temp,
      print_speed,
      retract_length,
      retract_speed,
      color,
      density,
      is_default
    )
  }

  public fun <T : Any> getById(id: Long, mapper: (
    id: Long,
    name: String,
    material: String,
    nozzle_temp: Long,
    bed_temp: Long,
    print_speed: Double,
    retract_length: Double,
    retract_speed: Double,
    color: String?,
    density: Double?,
    is_default: Long?,
  ) -> T): Query<T> = GetByIdQuery(id) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getDouble(5)!!,
      cursor.getDouble(6)!!,
      cursor.getDouble(7)!!,
      cursor.getString(8),
      cursor.getDouble(9),
      cursor.getLong(10)
    )
  }

  public fun getById(id: Long): Query<Filament_profiles> = getById(id) { id_, name, material,
      nozzle_temp, bed_temp, print_speed, retract_length, retract_speed, color, density,
      is_default ->
    Filament_profiles(
      id_,
      name,
      material,
      nozzle_temp,
      bed_temp,
      print_speed,
      retract_length,
      retract_speed,
      color,
      density,
      is_default
    )
  }

  public fun <T : Any> selectDefault(mapper: (
    id: Long,
    name: String,
    material: String,
    nozzle_temp: Long,
    bed_temp: Long,
    print_speed: Double,
    retract_length: Double,
    retract_speed: Double,
    color: String?,
    density: Double?,
    is_default: Long?,
  ) -> T): Query<T> = Query(1_863_973_382, arrayOf("filament_profiles"), driver,
      "FilamentQueries.sq", "selectDefault", """
  |SELECT filament_profiles.id, filament_profiles.name, filament_profiles.material, filament_profiles.nozzle_temp, filament_profiles.bed_temp, filament_profiles.print_speed, filament_profiles.retract_length, filament_profiles.retract_speed, filament_profiles.color, filament_profiles.density, filament_profiles.is_default FROM filament_profiles
  |WHERE is_default = 1
  |LIMIT 1
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getDouble(5)!!,
      cursor.getDouble(6)!!,
      cursor.getDouble(7)!!,
      cursor.getString(8),
      cursor.getDouble(9),
      cursor.getLong(10)
    )
  }

  public fun selectDefault(): Query<Filament_profiles> = selectDefault { id, name, material,
      nozzle_temp, bed_temp, print_speed, retract_length, retract_speed, color, density,
      is_default ->
    Filament_profiles(
      id,
      name,
      material,
      nozzle_temp,
      bed_temp,
      print_speed,
      retract_length,
      retract_speed,
      color,
      density,
      is_default
    )
  }

  public fun selectLastInsertRowId(): ExecutableQuery<Long> = Query(-1_671_686_229, driver,
      "FilamentQueries.sq", "selectLastInsertRowId", "SELECT last_insert_rowid() AS rowid") {
      cursor ->
    cursor.getLong(0)!!
  }

  public fun insert(
    name: String,
    material: String,
    nozzle_temp: Long,
    bed_temp: Long,
    print_speed: Double,
    retract_length: Double,
    retract_speed: Double,
    color: String?,
    density: Double?,
    is_default: Long?,
  ) {
    driver.execute(-513_578_632, """
        |INSERT INTO filament_profiles (
        |    name, material, nozzle_temp, bed_temp, print_speed,
        |    retract_length, retract_speed, color, density, is_default
        |)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 10) {
          bindString(0, name)
          bindString(1, material)
          bindLong(2, nozzle_temp)
          bindLong(3, bed_temp)
          bindDouble(4, print_speed)
          bindDouble(5, retract_length)
          bindDouble(6, retract_speed)
          bindString(7, color)
          bindDouble(8, density)
          bindLong(9, is_default)
        }
    notifyQueries(-513_578_632) { emit ->
      emit("filament_profiles")
    }
  }

  public fun update(
    name: String,
    material: String,
    nozzle_temp: Long,
    bed_temp: Long,
    print_speed: Double,
    retract_length: Double,
    retract_speed: Double,
    color: String?,
    density: Double?,
    is_default: Long?,
    id: Long,
  ) {
    driver.execute(-168_632_440, """
        |UPDATE filament_profiles
        |SET
        |    name = ?,
        |    material = ?,
        |    nozzle_temp = ?,
        |    bed_temp = ?,
        |    print_speed = ?,
        |    retract_length = ?,
        |    retract_speed = ?,
        |    color = ?,
        |    density = ?,
        |    is_default = ?
        |WHERE id = ?
        """.trimMargin(), 11) {
          bindString(0, name)
          bindString(1, material)
          bindLong(2, nozzle_temp)
          bindLong(3, bed_temp)
          bindDouble(4, print_speed)
          bindDouble(5, retract_length)
          bindDouble(6, retract_speed)
          bindString(7, color)
          bindDouble(8, density)
          bindLong(9, is_default)
          bindLong(10, id)
        }
    notifyQueries(-168_632_440) { emit ->
      emit("filament_profiles")
    }
  }

  public fun deleteById(id: Long) {
    driver.execute(1_977_136_988, """
        |DELETE FROM filament_profiles
        |WHERE id = ?
        """.trimMargin(), 1) {
          bindLong(0, id)
        }
    notifyQueries(1_977_136_988) { emit ->
      emit("filament_profiles")
    }
  }

  public fun deleteAll() {
    driver.execute(-1_321_696_041, """DELETE FROM filament_profiles""", 0)
    notifyQueries(-1_321_696_041) { emit ->
      emit("filament_profiles")
    }
  }

  private inner class GetByIdQuery<out T : Any>(
    public val id: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("filament_profiles", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("filament_profiles", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-773_851_799, """
    |SELECT filament_profiles.id, filament_profiles.name, filament_profiles.material, filament_profiles.nozzle_temp, filament_profiles.bed_temp, filament_profiles.print_speed, filament_profiles.retract_length, filament_profiles.retract_speed, filament_profiles.color, filament_profiles.density, filament_profiles.is_default FROM filament_profiles
    |WHERE id = ?
    """.trimMargin(), mapper, 1) {
      bindLong(0, id)
    }

    override fun toString(): String = "FilamentQueries.sq:getById"
  }
}
