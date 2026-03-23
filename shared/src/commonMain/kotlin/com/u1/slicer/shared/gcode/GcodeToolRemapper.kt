package com.u1.slicer.shared.gcode

import com.u1.slicer.shared.platform.file.FileReader

/**
 * Post-processes G-code to remap compact tool indices to actual printer slot indices.
 *
 * OrcaSlicer emits T0, T1, … (one per unique colour in the 3MF).  When the user assigns
 * model colours to non-zero slots (e.g. E3+E4 → physical slots [2,3]) we rewrite:
 *   T0 → T2,  T1 → T3
 *   M104 … T0 → M104 … T2   (set temperature, no wait)
 *   M109 … T0 → M109 … T2   (set temperature + wait)
 *
 * Mirrors bridge slicer.py remap_compacted_tools().
 */
object GcodeToolRemapper {

    private val TOOL_LINE_RE = Regex("""^T(\d+)\s*(?:;.*)?$""")
    // Captures the M104/M109 prefix up to (but not including) the T parameter
    private val MTEMP_T_RE = Regex("""(M10[49]\b.*?)\bT(\d+)""")
    // Snapmaker SM_ commands: SM_PRINT_AUTO_FEED EXTRUDER=N, SM_PRINT_FLOW_CALIBRATE INDEX=N, etc.
    private val SM_PARAM_RE = Regex("""((?:EXTRUDER|INDEX)=)(\d+)""")

    /**
     * Rewrite G-code content, replacing compact tool indices with [targetSlots].
     *
     * @param content  G-code file content to process.
     * @param targetSlots  Mapping from compact index → physical slot index.
     *                     E.g. [2, 3] means T0→T2, T1→T3.
     *                     If this is already the identity (0,1,…) nothing is written.
     * @return Remapped G-code content.
     */
    fun remapContent(content: String, targetSlots: List<Int>): String {
        if (targetSlots.isEmpty()) return content
        val toolMap = targetSlots.mapIndexed { compact, actual -> compact to actual }.toMap()

        return content.lineSequence().joinToString("\n") { line ->
            remapLine(line.trimEnd(), toolMap)
        }
    }

    /** Remap a single G-code line. Visible for testing. */
    fun remapLine(line: String, toolMap: Map<Int, Int>): String {
        // Standalone tool change: "T0", "T1 ; comment", …
        val tMatch = TOOL_LINE_RE.matchEntire(line)
        if (tMatch != null) {
            val compact = tMatch.groupValues[1].toIntOrNull() ?: return line
            val actual = toolMap[compact] ?: compact
            return "T$actual"
        }
        // M104/M109 with T parameter anywhere on the line
        if (MTEMP_T_RE.containsMatchIn(line)) {
            return MTEMP_T_RE.replace(line) { mr ->
                val compact = mr.groupValues[2].toIntOrNull() ?: return@replace mr.value
                val actual = toolMap[compact] ?: compact
                "${mr.groupValues[1]}T$actual"
            }
        }
        // Snapmaker SM_ commands with EXTRUDER=N or INDEX=N
        if (line.startsWith("SM_") && SM_PARAM_RE.containsMatchIn(line)) {
            return SM_PARAM_RE.replace(line) { mr ->
                val compact = mr.groupValues[2].toIntOrNull() ?: return@replace mr.value
                val actual = toolMap[compact] ?: compact
                "${mr.groupValues[1]}$actual"
            }
        }
        return line
    }
}
