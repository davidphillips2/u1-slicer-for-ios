package com.u1.slicer.shared.viewer

import com.u1.slicer.shared.platform.file.ZipFileReader
import com.u1.slicer.shared.platform.getLogger

/**
 * Parses mesh data from 3MF files for 3D preview rendering.
 * Platform-agnostic version supporting multi-extruder paint data.
 *
 * 3MF Format Overview:
 * - 3MF is a ZIP archive containing XML files
 * - Main model: 3D/3dmodel.model
 * - Mesh structure: vertices (x,y,z) + triangles (v1,v2,v3 indices)
 * - Multi-extruder support via paint_color or mmu_segmentation attributes
 */
object ThreeMfMeshParser {

    private val logger = getLogger()

    suspend fun parse(
        zipReader: ZipFileReader,
        path: String,
        extruderMap: Map<Int, Byte>? = null,
        detectedColorCount: Int = 0
    ): MeshData? {
        logger.i("ThreeMfMesh", "Parsing 3MF file")

        if (!zipReader.containsEntry(path, "3D/3dmodel.model")) {
            logger.w("ThreeMfMesh", "No 3D/3dmodel.model in ZIP")
            return null
        }

        try {
            val modelXml = zipReader.extractText(path, "3D/3dmodel.model")
            val parseResult = parseModelXml(modelXml)

            if (parseResult.vertices.isEmpty() || parseResult.triangles.isEmpty()) {
                logger.w("ThreeMfMesh", "No mesh data found")
                return null
            }

            return buildMeshData(parseResult, extruderMap, detectedColorCount)
        } catch (e: Exception) {
            logger.e("ThreeMfMesh", "Failed to parse 3MF: ${e.message}")
            return null
        }
    }

    private data class ModelParseResult(
        val vertices: FloatArray,
        val triangles: IntArray,
        val paintSpecs: Array<String?>?,
        val hasPaintData: Boolean
    )

    /**
     * Parse the 3MF model XML line-by-line.
     * Uses indexOf-based parsing for memory efficiency with large files.
     */
    private fun parseModelXml(xml: String): ModelParseResult {
        val vertList = mutableListOf<Float>()
        val triList = mutableListOf<Int>()
        val paintList = mutableListOf<String?>()
        var hasPaintData = false
        var inVertices = false
        var inTriangles = false
        var currentTriIndex = 0

        for (line in xml.lines()) {
            // Fast path: mesh data lines are the vast majority
            if (inVertices) {
                if (line.contains("<vertex")) {
                    parseVertexLine(line, vertList)
                }
                if (line.contains("</vertices>")) {
                    inVertices = false
                }
            }
            if (inTriangles) {
                if (line.contains("<triangle")) {
                    parseTriangleLine(line, triList, currentTriIndex)

                    // Extract paint data for multi-extruder models
                    val spec = extractAttr(line, "paint_color")
                        ?: extractAttr(line, "mmu_segmentation")
                        ?: extractAttr(line, "slic3rpe:mmu_segmentation")

                    if (spec != null) {
                        hasPaintData = true
                        paintList.add(spec)
                    } else {
                        paintList.add(null)
                    }
                    currentTriIndex++
                }
                if (line.contains("</triangles>")) {
                    inTriangles = false
                }
            }

            // Structural tags
            if (!inVertices && !inTriangles) {
                when {
                    line.contains("<vertices>") -> inVertices = true
                    line.contains("<triangles>") -> inTriangles = true
                }
            }
        }

        return ModelParseResult(
            vertices = vertList.toFloatArray(),
            triangles = triList.toIntArray(),
            paintSpecs = if (paintList.any { it != null }) paintList.toTypedArray() else null,
            hasPaintData = hasPaintData
        )
    }

    private fun parseVertexLine(line: String, verts: MutableList<Float>) {
        val x = extractAttr(line, "x")?.toFloatOrNull() ?: return
        val y = extractAttr(line, "y")?.toFloatOrNull() ?: return
        val z = extractAttr(line, "z")?.toFloatOrNull() ?: return
        verts.addAll(listOf(x, y, z))
    }

    private fun parseTriangleLine(line: String, tris: MutableList<Int>, triIndex: Int) {
        val v1 = extractAttr(line, "v1")?.toIntOrNull() ?: return
        val v2 = extractAttr(line, "v2")?.toIntOrNull() ?: return
        val v3 = extractAttr(line, "v3")?.toIntOrNull() ?: return
        tris.addAll(listOf(v1, v2, v3))
    }

    private fun extractAttr(line: String, attrName: String): String? {
        val pattern = "$attrName=\""
        val start = line.indexOf(pattern)
        if (start < 0) return null
        val valueStart = start + pattern.length
        val end = line.indexOf("\"", valueStart)
        if (end < 0) return null
        return line.substring(valueStart, end)
    }

    private fun buildMeshData(
        result: ModelParseResult,
        extruderMap: Map<Int, Byte>?,
        detectedColorCount: Int
    ): MeshData {
        val triCount = result.triangles.size / 3
        val vertexArray = MeshData.allocateArray(triCount)

        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

        // Build triangle mesh
        for (tri in 0 until triCount) {
            val i1 = result.triangles[tri * 3]
            val i2 = result.triangles[tri * 3 + 1]
            val i3 = result.triangles[tri * 3 + 2]

            // Get vertices
            val v1 = getVertex(result.vertices, i1)
            val v2 = getVertex(result.vertices, i2)
            val v3 = getVertex(result.vertices, i3)

            // Calculate normal (simple face normal)
            val normal = calculateNormal(v1, v2, v3)

            // Add 3 vertices for this triangle
            val vertices = listOf(v1, v2, v3)
            for (v in vertices) {
                val (x, y, z) = v
                minX = minOf(minX, x)
                minY = minOf(minY, y)
                minZ = minOf(minZ, z)
                maxX = maxOf(maxX, x)
                maxY = maxOf(maxY, y)
                maxZ = maxOf(maxZ, z)
            }

            // Write vertices to array
            for (vertexIndex in vertices.indices) {
                val v = vertices[vertexIndex]
                val offset = (tri * 3 + vertexIndex) * MeshData.FLOATS_PER_VERTEX
                vertexArray[offset + 0] = v.first
                vertexArray[offset + 1] = v.second
                vertexArray[offset + 2] = v.third
                vertexArray[offset + 3] = normal.first
                vertexArray[offset + 4] = normal.second
                vertexArray[offset + 5] = normal.third
                vertexArray[offset + 6] = 0.7f
                vertexArray[offset + 7] = 0.7f
                vertexArray[offset + 8] = 0.7f
                vertexArray[offset + 9] = 1.0f
            }
        }

        // Parse extruder indices from paint specs
        val extruderIndices = if (result.paintSpecs != null && result.paintSpecs.isNotEmpty()) {
            parseExtruderIndices(result.paintSpecs, extruderMap, detectedColorCount)
        } else null

        return MeshData(
            vertices = vertexArray,
            vertexCount = triCount * 3,
            minX = minX, minY = minY, minZ = minZ,
            maxX = maxX, maxY = maxY, maxZ = maxZ,
            extruderIndices = extruderIndices
        )
    }

    private fun getVertex(verts: FloatArray, index: Int): Triple<Float, Float, Float> {
        val i = index * 3
        return Triple(verts[i], verts[i + 1], verts[i + 2])
    }

    private fun calculateNormal(
        v1: Triple<Float, Float, Float>,
        v2: Triple<Float, Float, Float>,
        v3: Triple<Float, Float, Float>
    ): Triple<Float, Float, Float> {
        val ax = v2.first - v1.first
        val ay = v2.second - v1.second
        val az = v2.third - v1.third
        val bx = v3.first - v1.first
        val by = v3.second - v1.second
        val bz = v3.third - v1.third

        val nx = ay * bz - az * by
        val ny = az * bx - ax * bz
        val nz = ax * by - ay * bx

        // Normalize
        val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)
        return if (len > 0) {
            Triple(nx / len, ny / len, nz / len)
        } else {
            Triple(0f, 0f, 1f)
        }
    }

    /**
     * Parse paint_color specifications into per-triangle extruder indices.
     * Supports both SEMM (paint_color) and MMU (mmu_segmentation) formats.
     */
    private fun parseExtruderIndices(
        paintSpecs: Array<String?>,
        extruderMap: Map<Int, Byte>?,
        detectedColorCount: Int
    ): ByteArray {
        val indices = ByteArray(paintSpecs.size)

        for (i in paintSpecs.indices) {
            val spec = paintSpecs[i]
            if (spec == null) {
                indices[i] = 0 // Default to extruder 0
                continue
            }

            indices[i] = when {
                // SEMM format: paint_color="#FF00FF" (RGB hex)
                spec.startsWith("#") && spec.length >= 7 -> {
                    val colorInt = spec.substring(1).toIntOrNull(16) ?: 0
                    // Map RGB to nearest extruder (simplified)
                    mapColorToExtruder(colorInt, detectedColorCount)
                }
                // MMU format: mmu_segmentation="0" or similar
                else -> {
                    val extruderNum = spec.toIntOrNull() ?: 0
                    // Apply extruder mapping if provided
                    extruderMap?.get(extruderNum)?.toInt() ?: extruderNum
                }
            }.toByte()
        }

        return indices
    }

    private fun mapColorToExtruder(color: Int, colorCount: Int): Int {
        // Simplified color mapping - distribute colors evenly
        return when (colorCount) {
            0, 1 -> 0
            2 -> if (color < 0x800000) 0 else 1
            else -> (color % colorCount)
        }
    }
}
