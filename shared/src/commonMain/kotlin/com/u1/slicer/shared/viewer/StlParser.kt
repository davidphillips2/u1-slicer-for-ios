package com.u1.slicer.shared.viewer

import com.u1.slicer.shared.platform.file.FileReader

/**
 * Parses binary and ASCII STL files into MeshData.
 * Platform-agnostic version using ByteArray operations instead of ByteBuffer.
 */
object StlParser {

    suspend fun parse(reader: FileReader, path: String): MeshData {
        val bytes = reader.readBytes(path)
        return if (isBinaryStl(bytes)) {
            parseBinary(bytes)
        } else {
            parseAscii(bytes)
        }
    }

    private fun isBinaryStl(bytes: ByteArray): Boolean {
        if (bytes.size < 84) return false
        // Check if starts with "solid " — could be ASCII
        val headerSize = minOf(80, bytes.size)
        val header = bytes.decodeToString(0, headerSize)
        if (header.trimStart().startsWith("solid")) {
            // But binary STLs can also start with "solid" in header
            // Check if the declared triangle count matches file size
            val triangleCount = bytes.getIntAt(80)
            val expectedSize = 84L + triangleCount * 50L
            if (expectedSize == bytes.size.toLong()) return true
            // If file has "facet" keyword, it's likely ASCII
            val sampleSize = minOf(1024, bytes.size)
            val sample = bytes.decodeToString(0, sampleSize)
            return !sample.contains("facet")
        }
        return true
    }

    private fun parseBinary(bytes: ByteArray): MeshData {
        var pos = 80  // Skip header
        val triangleCount = bytes.getIntAt(pos)
        pos += 4

        val vertexArray = MeshData.allocateArray(triangleCount)

        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

        for (i in 0 until triangleCount) {
            // Normal vector (we read but don't use it for now)
            val nx = bytes.getFloatAt(pos); pos += 4
            val ny = bytes.getFloatAt(pos); pos += 4
            val nz = bytes.getFloatAt(pos); pos += 4

            // 3 vertices per triangle
            for (v in 0 until 3) {
                val x = bytes.getFloatAt(pos); pos += 4
                val y = bytes.getFloatAt(pos); pos += 4
                val z = bytes.getFloatAt(pos); pos += 4

                val vertexOffset = (i * 3 + v) * MeshData.FLOATS_PER_VERTEX
                vertexArray[vertexOffset + 0] = x
                vertexArray[vertexOffset + 1] = y
                vertexArray[vertexOffset + 2] = z
                vertexArray[vertexOffset + 3] = nx
                vertexArray[vertexOffset + 4] = ny
                vertexArray[vertexOffset + 5] = nz
                vertexArray[vertexOffset + 6] = 0.7f
                vertexArray[vertexOffset + 7] = 0.7f
                vertexArray[vertexOffset + 8] = 0.7f
                vertexArray[vertexOffset + 9] = 1.0f

                if (x < minX) minX = x; if (y < minY) minY = y; if (z < minZ) minZ = z
                if (x > maxX) maxX = x; if (y > maxY) maxY = y; if (z > maxZ) maxZ = z
            }

            pos += 2  // Skip attribute byte count
        }

        return MeshData(
            vertices = vertexArray,
            vertexCount = triangleCount * 3,
            minX = minX, minY = minY, minZ = minZ,
            maxX = maxX, maxY = maxY, maxZ = maxZ,
            extruderIndices = null
        )
    }

    private fun parseAscii(bytes: ByteArray): MeshData {
        val text = bytes.decodeToString()
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()

        var nx = 0f; var ny = 0f; var nz = 0f
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

        val normalRegex = Regex("""facet\s+normal\s+([-\d.eE+]+)\s+([-\d.eE+]+)\s+([-\d.eE+]+)""")
        val vertexRegex = Regex("""vertex\s+([-\d.eE+]+)\s+([-\d.eE+]+)\s+([-\d.eE+]+)""")

        for (line in text.lines()) {
            val trimmed = line.trim()
            val normalMatch = normalRegex.find(trimmed)
            if (normalMatch != null) {
                nx = normalMatch.groupValues[1].toFloat()
                ny = normalMatch.groupValues[2].toFloat()
                nz = normalMatch.groupValues[3].toFloat()
            } else {
                val vertexMatch = vertexRegex.find(trimmed)
                if (vertexMatch != null) {
                    val x = vertexMatch.groupValues[1].toFloat()
                    val y = vertexMatch.groupValues[2].toFloat()
                    val z = vertexMatch.groupValues[3].toFloat()
                    vertices.addAll(listOf(x, y, z))
                    normals.addAll(listOf(nx, ny, nz))

                    if (x < minX) minX = x; if (y < minY) minY = y; if (z < minZ) minZ = z
                    if (x > maxX) maxX = x; if (y > maxY) maxY = y; if (z > maxZ) maxZ = z
                }
            }
        }

        val triangleCount = vertices.size / 9
        val vertexArray = MeshData.allocateArray(triangleCount)
        for (i in 0 until vertices.size / 3) {
            val vertexOffset = i * MeshData.FLOATS_PER_VERTEX
            vertexArray[vertexOffset + 0] = vertices[i * 3]
            vertexArray[vertexOffset + 1] = vertices[i * 3 + 1]
            vertexArray[vertexOffset + 2] = vertices[i * 3 + 2]
            vertexArray[vertexOffset + 3] = normals[i * 3]
            vertexArray[vertexOffset + 4] = normals[i * 3 + 1]
            vertexArray[vertexOffset + 5] = normals[i * 3 + 2]
            vertexArray[vertexOffset + 6] = 0.7f
            vertexArray[vertexOffset + 7] = 0.7f
            vertexArray[vertexOffset + 8] = 0.7f
            vertexArray[vertexOffset + 9] = 1.0f
        }

        return MeshData(
            vertices = vertexArray,
            vertexCount = vertices.size / 3,
            minX = minX, minY = minY, minZ = minZ,
            maxX = maxX, maxY = maxY, maxZ = maxZ,
            extruderIndices = null
        )
    }
}

// Extension functions for ByteArray to read little-endian values
private fun ByteArray.getIntAt(offset: Int): Int {
    return (this[offset].toInt() and 0xFF) or
           ((this[offset + 1].toInt() and 0xFF) shl 8) or
           ((this[offset + 2].toInt() and 0xFF) shl 16) or
           ((this[offset + 3].toInt() and 0xFF) shl 24)
}

private fun ByteArray.getFloatAt(offset: Int): Float {
    return Float.fromBits(this.getIntAt(offset))
}
