//
//  Model3DView.swift
//  U1Slicer
//
//  SwiftUI wrapper for Metal 3D model rendering
//

import SwiftUI
import MetalKit

struct Model3DView: UIViewRepresentable {
    let meshData: MeshData3D
    let extruderColors: [Color]

    func makeUIView(context: Context) -> MTKView {
        let view = MTKView()
        view.device = MTLCreateSystemDefaultDevice()
        view.clearColor = MTLClearColor(
            red: 0.059,
            green: 0.059,
            blue: 0.118,
            alpha: 1.0
        )
        view.enableSetNeedsDisplay = false

        // Create and attach renderer
        let renderer = MetalRenderer3D()
        renderer.setMesh(meshData: meshData)
        renderer.setExtruderColors(extruderColors)
        view.delegate = renderer

        context.coordinator.renderer = renderer
        context.coordinator.view = view

        return view
    }

    func updateUIView(_ view: MTKView, context: Context) {
        // Update mesh if changed
        if let renderer = context.coordinator.renderer {
            renderer.setMesh(meshData: meshData)
            renderer.setExtruderColors(extruderColors)
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    class Coordinator {
        var renderer: MetalRenderer3D?
        var view: MTKView?
    }
}

// ============================================================================
// MARK: - Mesh Data Model
// ============================================================================

struct MeshData3D {
    let vertices: [Float]
    let vertexCount: Int
    let extruderIndices: [UInt8]?

    var size: Int {
        return vertices.count * MemoryLayout<Float>.size
    }
}

// ============================================================================
// MARK: - Metal Renderer (placeholder)
// ============================================================================

class MetalRenderer3D: NSObject, MTKViewDelegate {
    var device: MTLDevice?
    var commandQueue: MTLCommandQueue?
    var pipelineState: MTLRenderPipelineState?

    private var vertexBuffer: MTLBuffer?
    private var vertexCount: Int = 0

    override init() {
        super.init()
        device = MTLCreateSystemDefaultDevice()
        commandQueue = device?.makeCommandQueue()
    }

    func setMesh(meshData: MeshData3D) {
        guard let device = device else { return }

        vertexCount = meshData.vertexCount
        vertexBuffer = device.makeBuffer(
            bytes: meshData.vertices,
            length: meshData.size,
            options: []
        )
    }

    func setExtruderColors(_ colors: [Color]) {
        // TODO: Set extruder colors for rendering
    }

    func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {
        // Handle view size changes
    }

    func draw(in view: MTKView) {
        guard let drawable = view.currentDrawable,
              let descriptor = view.currentRenderPassDescriptor,
              let commandBuffer = commandQueue?.makeCommandBuffer(),
              let commandEncoder = commandBuffer.makeRenderCommandEncoder(descriptor: descriptor) else {
            return
        }

        // TODO: Implement actual Metal rendering
        // This is a placeholder that just clears the screen

        commandEncoder.endEncoding()
        commandBuffer.present(drawable)
        commandBuffer.commit()
    }
}
