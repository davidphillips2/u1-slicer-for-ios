package com.u1.slicer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparePreviewPlacementTest {

    @Test
    fun `native 3mf preview keeps object placement and wipe tower available`() {
        val config = buildPreparePreviewPlacementConfig(
            nativeThreeMfPreview = true,
            objectPositionsPresent = true,
            onPositionsChangedPresent = true,
            wipeTowerEnabled = true
        )

        assertTrue(config.objectPlacementEnabled)
        assertTrue(config.wipeTowerVisible)
    }

    @Test
    fun `stl preview keeps wipe tower tied to object placement mode`() {
        val config = buildPreparePreviewPlacementConfig(
            nativeThreeMfPreview = false,
            objectPositionsPresent = true,
            onPositionsChangedPresent = true,
            wipeTowerEnabled = true
        )

        assertTrue(config.objectPlacementEnabled)
        assertTrue(config.wipeTowerVisible)
    }

    @Test
    fun `wipe tower still shows when placement callbacks are absent`() {
        val config = buildPreparePreviewPlacementConfig(
            nativeThreeMfPreview = true,
            objectPositionsPresent = false,
            onPositionsChangedPresent = false,
            wipeTowerEnabled = true
        )

        assertFalse(config.objectPlacementEnabled)
        assertTrue(config.wipeTowerVisible)
    }
}
