package com.u1.slicer.shared.database

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Slice_jobs(
  public val id: Long,
  public val model_name: String,
  public val gcode_path: String,
  public val total_layers: Long,
  public val estimated_time_seconds: Double,
  public val estimated_filament_mm: Double,
  public val layer_height: Double,
  public val fill_density: Double,
  public val nozzle_temp: Long,
  public val bed_temp: Long,
  public val support_enabled: Long,
  public val filament_type: String,
  public val timestamp: Long,
)
