package com.u1.slicer.viewer

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelViewerViewTest {

    @Test
    fun `resolveDragTarget falls back to bed plane when face-plane miss occurs`() {
        val primaryMiss = floatArrayOf(140f, 140f)
        val bedPlaneHit = floatArrayOf(125f, 125f)

        val result = ModelViewerView.resolveDragTarget(primaryMiss, bedPlaneHit) { x, y ->
            if (x == 125f && y == 125f) 2 else -1
        }

        assertEquals(2, result)
    }

    @Test
    fun `resolveDragTarget keeps face-plane hit when it succeeds`() {
        val primaryHit = floatArrayOf(128f, 126f)
        val bedPlaneHit = floatArrayOf(125f, 125f)

        val result = ModelViewerView.resolveDragTarget(primaryHit, bedPlaneHit) { x, y ->
            when {
                x == 128f && y == 126f -> 1
                x == 125f && y == 125f -> 2
                else -> -1
            }
        }

        assertEquals(1, result)
    }

    @Test
    fun `resolveDragTarget returns none when both projections miss`() {
        val result = ModelViewerView.resolveDragTarget(
            floatArrayOf(150f, 150f),
            floatArrayOf(160f, 160f)
        ) { _, _ -> -1 }

        assertEquals(-1, result)
    }
}
