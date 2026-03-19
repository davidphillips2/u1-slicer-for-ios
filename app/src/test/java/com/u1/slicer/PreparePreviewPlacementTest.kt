package com.u1.slicer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparePreviewPlacementTest {

    @Test
    fun `native 3mf preview keeps wipe tower visible even without object placement`() {
        val config = buildPreparePreviewPlacementConfig(
            nativeThreeMfPreview = true,
            objectPositionsPresent = true,
            onPositionsChangedPresent = true,
            wipeTowerEnabled = true
        )

        assertFalse(config.objectPlacementEnabled)
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
}
