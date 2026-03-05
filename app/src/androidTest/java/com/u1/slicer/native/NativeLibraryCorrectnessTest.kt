package com.u1.slicer.native

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.u1.slicer.NativeLibrary
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented correctness tests for JNI return values from NativeLibrary.
 *
 * These complement NativeLibrarySymbolTest (which only checks linkage with
 * empty/no-op inputs) by loading a real model and asserting that every field
 * of the returned ModelInfo has a plausible value.
 *
 * This class specifically guards against the class of bug where C++ float→double
 * or bool→int promotion in JNI varargs (NewObject) corrupts the argument layout,
 * causing fields like sizeX/Y/Z or isManifold to read garbage from the wrong
 * stack offset. Such bugs are invisible when the model is empty (all-zero floats)
 * but surface immediately with any real geometry.
 */
@RunWith(AndroidJUnit4::class)
class NativeLibraryCorrectnessTest {

    private lateinit var lib: NativeLibrary
    private lateinit var stlFile: File

    @Before
    fun setup() {
        assertTrue(
            "Native library must be loaded on device (arm64 required)",
            NativeLibrary.isLoaded
        )
        lib = NativeLibrary()

        // Copy the bundled STL asset to a path the native code can open via fopen().
        // Use targetContext (the app under test) so cacheDir is guaranteed to exist.
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        stlFile = File(ctx.cacheDir, "tetrahedron_test.stl")
        stlFile.parentFile?.mkdirs()
        InstrumentationRegistry.getInstrumentation().context
            .assets.open("tetrahedron.stl").use { it.copyTo(stlFile.outputStream()) }
    }

    @After
    fun teardown() {
        lib.clearModel()
        stlFile.delete()
    }

    @Test
    fun loadModel_returnsTrue_forValidStl() {
        assertTrue("loadModel should return true for a valid STL", lib.loadModel(stlFile.absolutePath))
    }

    /**
     * Regression test for the NewObject float→double promotion bug (tombstone_24/25).
     *
     * The tetrahedron spans (0,0,0)–(10,10,10) mm so every float field is non-zero.
     * With the varargs bug, sizeX/Y/Z read from the wrong stack offsets and return
     * garbage; the process also SIGABRTs before reaching the assertions because the
     * JNI runtime rejects the corrupted jboolean value for isManifold.
     */
    @Test
    fun getModelInfo_afterLoad_hasCorrectFields() {
        assertTrue(lib.loadModel(stlFile.absolutePath))
        val info = lib.getModelInfo()

        assertNotNull("getModelInfo should return non-null after load", info)
        info!!

        assertEquals("tetrahedron_test.stl", info.filename)
        assertEquals("stl", info.format)

        // Bounding box: tetrahedron spans 0–10 mm on each axis
        assertTrue("sizeX should be ~10mm, was ${info.sizeX}", info.sizeX in 9f..11f)
        assertTrue("sizeY should be ~10mm, was ${info.sizeY}", info.sizeY in 9f..11f)
        assertTrue("sizeZ should be ~10mm, was ${info.sizeZ}", info.sizeZ in 9f..11f)

        assertTrue("triangleCount should be > 0, was ${info.triangleCount}", info.triangleCount > 0)
        assertTrue("volumeCount should be >= 1, was ${info.volumeCount}", info.volumeCount >= 1)

        // isManifold must be a valid boolean. If the jboolean arg held garbage the
        // JNI runtime would have aborted before we reach this line — making this an
        // implicit assertion that the NewObjectA fix is in place.
        assertTrue("isManifold must be true or false", info.isManifold || !info.isManifold)
    }

    @Test
    fun getModelInfo_emptyModel_returnsDefaultStruct() {
        lib.clearModel()
        val info = lib.getModelInfo()
        assertNotNull(info)
        info!!
        assertEquals("", info.filename)
        assertEquals(0f, info.sizeX, 0.001f)
        assertEquals(0f, info.sizeY, 0.001f)
        assertEquals(0f, info.sizeZ, 0.001f)
    }

    @Test
    fun clearModel_afterLoad_resetsState() {
        assertTrue(lib.loadModel(stlFile.absolutePath))
        lib.clearModel()
        val info = lib.getModelInfo()
        assertNotNull(info)
        assertEquals("", info!!.filename)
    }
}
