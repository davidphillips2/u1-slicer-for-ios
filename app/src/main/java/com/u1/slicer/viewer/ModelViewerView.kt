package com.u1.slicer.viewer

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.sqrt

class ModelViewerView(context: Context) : GLSurfaceView(context) {

    val renderer = ModelRenderer(context)
    private val scaleDetector: ScaleGestureDetector

    private var previousX = 0f
    private var previousY = 0f
    private var pointerCount = 0
    private var previousDist = 0f
    private var previousMidX = 0f
    private var previousMidY = 0f

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.camera.zoom(1f / detector.scaleFactor)
                requestRender()
                return true
            }
        })
    }

    fun setMesh(mesh: MeshData) {
        renderer.pendingMesh = mesh
        requestRender()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                previousX = event.x
                previousY = event.y
                pointerCount = 1
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                pointerCount = event.pointerCount
                if (pointerCount >= 2) {
                    previousMidX = (event.getX(0) + event.getX(1)) / 2
                    previousMidY = (event.getY(0) + event.getY(1)) / 2
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerCount == 1 && event.pointerCount == 1) {
                    // Single finger: rotate
                    val dx = event.x - previousX
                    val dy = event.y - previousY

                    renderer.camera.rotate(-dx * 0.3f, dy * 0.3f)
                    requestRender()

                    previousX = event.x
                    previousY = event.y
                } else if (event.pointerCount >= 2) {
                    // Two fingers: pan
                    val midX = (event.getX(0) + event.getX(1)) / 2
                    val midY = (event.getY(0) + event.getY(1)) / 2

                    val dx = midX - previousMidX
                    val dy = midY - previousMidY

                    val panScale = renderer.camera.distance * 0.002f
                    renderer.camera.pan(-dx * panScale, dy * panScale)
                    requestRender()

                    previousMidX = midX
                    previousMidY = midY
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                pointerCount = event.pointerCount - 1
                if (event.pointerCount <= 1) {
                    previousX = event.x
                    previousY = event.y
                }
            }
        }
        return true
    }
}
