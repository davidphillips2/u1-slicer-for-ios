package com.u1.slicer.viewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRendererCameraTest {

    @Test
    fun `computePreviewFitDistance zooms closer for smaller models`() {
        val dragonDistance = ModelRenderer.computePreviewFitDistance(80.748f, 40.249f, 20.0005f)
        val fullBedDistance = ModelRenderer.computePreviewFitDistance(270f, 270f, 20f)

        assertTrue(
            "Dragon-sized plate preview should frame closer than full bed, got dragon=$dragonDistance bed=$fullBedDistance",
            dragonDistance < fullBedDistance
        )
        assertTrue(
            "Dragon-sized plate preview should be framed much closer than the old fixed 500 distance, got $dragonDistance",
            dragonDistance < 200f
        )
    }

    @Test
    fun `computePreviewFitDistance keeps tiny models readable`() {
        val cubeDistance = ModelRenderer.computePreviewFitDistance(10f, 10f, 10f)
        assertEquals(90f, cubeDistance, 0.001f)
    }

    @Test
    fun `computePreviewFitDistance keeps full bed models comfortably framed`() {
        val fullBedDistance = ModelRenderer.computePreviewFitDistance(270f, 270f, 20f)
        assertTrue(
            "Full-bed models still need a comfortable margin, got $fullBedDistance",
            fullBedDistance in 450f..500f
        )
    }
}
