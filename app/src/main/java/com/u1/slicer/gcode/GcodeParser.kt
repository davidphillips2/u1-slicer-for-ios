package com.u1.slicer.gcode

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

object GcodeParser {

    fun parse(file: File): ParsedGcode {
        val layers = mutableListOf<GcodeLayer>()
        var currentMoves = mutableListOf<GcodeMove>()
        var currentZ = 0f
        var layerIndex = 0

        var x = 0f
        var y = 0f
        var currentExtruder = 0
        var lastE = 0f
        var absoluteE = true

        BufferedReader(FileReader(file)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line!!.trim()
                if (l.isEmpty() || l.startsWith(';')) {
                    // Check for layer change comment
                    if (l.startsWith(";LAYER_CHANGE") || l.startsWith("; layer_change")) {
                        if (currentMoves.isNotEmpty()) {
                            layers.add(GcodeLayer(layerIndex, currentZ, currentMoves.toList()))
                            layerIndex++
                            currentMoves = mutableListOf()
                        }
                    }
                    continue
                }

                val cmd = l.substringBefore(';').trim()
                if (cmd.isEmpty()) continue

                val parts = cmd.split(' ')
                val code = parts[0]

                when (code) {
                    "G0", "G1" -> {
                        var newX = x
                        var newY = y
                        var newZ = currentZ
                        var newE: Float? = null

                        for (i in 1 until parts.size) {
                            val p = parts[i]
                            if (p.isEmpty()) continue
                            val value = p.substring(1).toFloatOrNull() ?: continue
                            when (p[0]) {
                                'X' -> newX = value
                                'Y' -> newY = value
                                'Z' -> newZ = value
                                'E' -> newE = value
                            }
                        }

                        // Detect Z layer change
                        if (newZ != currentZ && currentMoves.isNotEmpty()) {
                            layers.add(GcodeLayer(layerIndex, currentZ, currentMoves.toList()))
                            layerIndex++
                            currentMoves = mutableListOf()
                            currentZ = newZ
                        } else if (newZ != currentZ) {
                            currentZ = newZ
                        }

                        // Determine if this is an extrusion move
                        val isExtrude = if (newE != null) {
                            if (absoluteE) {
                                newE > lastE
                            } else {
                                newE > 0
                            }
                        } else {
                            false
                        }

                        if (newE != null) {
                            lastE = newE
                        }

                        // Only add moves with actual XY displacement
                        if (newX != x || newY != y) {
                            currentMoves.add(
                                GcodeMove(
                                    type = if (isExtrude) MoveType.EXTRUDE else MoveType.TRAVEL,
                                    x0 = x, y0 = y,
                                    x1 = newX, y1 = newY,
                                    extruder = currentExtruder
                                )
                            )
                        }

                        x = newX
                        y = newY
                    }
                    "G92" -> {
                        // Reset E position
                        for (i in 1 until parts.size) {
                            val p = parts[i]
                            if (p.startsWith("E")) {
                                lastE = p.substring(1).toFloatOrNull() ?: 0f
                            }
                        }
                    }
                    "M82" -> absoluteE = true
                    "M83" -> absoluteE = false
                    "T0" -> currentExtruder = 0
                    "T1" -> currentExtruder = 1
                    "T2" -> currentExtruder = 2
                    "T3" -> currentExtruder = 3
                }
            }
        }

        // Don't forget the last layer
        if (currentMoves.isNotEmpty()) {
            layers.add(GcodeLayer(layerIndex, currentZ, currentMoves.toList()))
        }

        return ParsedGcode(layers = layers)
    }
}
