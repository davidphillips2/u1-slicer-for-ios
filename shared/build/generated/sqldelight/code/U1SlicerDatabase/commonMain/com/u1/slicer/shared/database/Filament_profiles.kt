package com.u1.slicer.shared.database

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Filament_profiles(
  public val id: Long,
  public val name: String,
  public val material: String,
  public val nozzle_temp: Long,
  public val bed_temp: Long,
  public val print_speed: Double,
  public val retract_length: Double,
  public val retract_speed: Double,
  public val color: String?,
  public val density: Double?,
  public val is_default: Long?,
)
