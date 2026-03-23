package com.u1.slicer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilePickerValidationTest {

    @Test
    fun `3mf files are supported`() {
        assertTrue(SlicerViewModel.isSupportedFile("model.3mf"))
        assertTrue(SlicerViewModel.isSupportedFile("My Model (1).3mf"))
        assertTrue(SlicerViewModel.isSupportedFile("UPPERCASE.3MF"))
    }

    @Test
    fun `stl files are supported`() {
        assertTrue(SlicerViewModel.isSupportedFile("benchy.stl"))
        assertTrue(SlicerViewModel.isSupportedFile("part.STL"))
    }

    @Test
    fun `obj files are supported`() {
        assertTrue(SlicerViewModel.isSupportedFile("mesh.obj"))
        assertTrue(SlicerViewModel.isSupportedFile("MESH.OBJ"))
    }

    @Test
    fun `step files are supported`() {
        assertTrue(SlicerViewModel.isSupportedFile("cad-part.step"))
        assertTrue(SlicerViewModel.isSupportedFile("assembly.stp"))
        assertTrue(SlicerViewModel.isSupportedFile("model.STEP"))
        assertTrue(SlicerViewModel.isSupportedFile("model.STP"))
    }

    @Test
    fun `unsupported file types are rejected`() {
        assertFalse(SlicerViewModel.isSupportedFile("photo.jpg"))
        assertFalse(SlicerViewModel.isSupportedFile("document.pdf"))
        assertFalse(SlicerViewModel.isSupportedFile("archive.zip"))
        assertFalse(SlicerViewModel.isSupportedFile("text.txt"))
        assertFalse(SlicerViewModel.isSupportedFile("spreadsheet.xlsx"))
        assertFalse(SlicerViewModel.isSupportedFile("video.mp4"))
        assertFalse(SlicerViewModel.isSupportedFile("code.gcode"))
    }

    @Test
    fun `files without extension are rejected`() {
        assertFalse(SlicerViewModel.isSupportedFile("noextension"))
        assertFalse(SlicerViewModel.isSupportedFile(""))
    }

    @Test
    fun `extension matching is case insensitive`() {
        assertTrue(SlicerViewModel.isSupportedFile("mixed.3Mf"))
        assertTrue(SlicerViewModel.isSupportedFile("mixed.Stl"))
        assertTrue(SlicerViewModel.isSupportedFile("mixed.OBJ"))
    }

    @Test
    fun `files with dots in name match on last extension`() {
        assertTrue(SlicerViewModel.isSupportedFile("my.model.v2.3mf"))
        assertFalse(SlicerViewModel.isSupportedFile("model.3mf.zip"))
    }

    @Test
    fun `download metadata suffix is stripped before extension checks`() {
        val normalized = SlicerViewModel.normalizeIncomingFilename(
            "super+clean.3mf;filename*=utf-8''super+clean.3mf"
        )
        assertTrue(SlicerViewModel.isSupportedFile(normalized))
        assertTrue(normalized.endsWith(".3mf"))
    }
}
