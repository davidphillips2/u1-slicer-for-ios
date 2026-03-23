//
//  MetalMeshRenderer.swift
//  U1Slicer
//
//  Metal-based 3D mesh renderer for iOS
//  Implements the C API for Kotlin/Native interop
//

import Foundation
import Metal
import MetalKit
import simd

// ============================================================================
// MARK: - Metal Mesh Renderer (Swift Implementation)
// ============================================================================

/// Metal-based 3D mesh renderer for U1 Slicer
public class MetalMeshRenderer {

    // MARK: - Properties

    private var device: MTLDevice
    private var commandQueue: MTLCommandQueue
    private var pipelineState: MTLRenderPipelineState?
    private var depthStencilState: MTLDepthStencilState

    // Mesh data
    private var vertexBuffer: MTLBuffer?
    private var vertexCount: Int = 0
    private var extruderIndices: Data?
    private var extruderCount: Int = 0

    // Instance data
    private var instanceColors: [SIMD4<Float>] = []
    private var instancePositions: [SIMD2<Float>] = []

    // Transform
    private var modelScale: SIMD3<Float> = SIMD3<Float>(1, 1, 1)
    private var highlightIndex: Int = -1

    // Wipe tower
    private var wipeTower: (x: Float, y: Float, width: Float, depth: Float)? = nil

    // Camera
    private var cameraDistance: Float = 400
    private var cameraYaw: Float = 45.0 * (.pi / 180.0)
    private var cameraPitch: Float = 30.0 * (.pi / 180.0)

    // Uniforms
    private struct Uniforms {
        var modelViewProjectionMatrix: matrix_float4x4
        var modelMatrix: matrix_float4x4
        var normalMatrix: matrix_float3x3
        var lightDirection: SIMD3<Float>
        var ambientIntensity: Float
        var directionalIntensity: Float
        var useVertexColor: UInt32
        var highlightIndex: Int32
    }

    // MARK: - Initialization

    public init?() {
        guard let device = MTLCreateSystemDefaultDevice() else {
            print("Metal is not supported on this device")
            return nil
        }

        self.device = device
        self.commandQueue = device.makeCommandQueue()!

        // Create depth stencil state
        let depthDescriptor = MTLDepthStencilDescriptor()
        depthDescriptor.depthCompareFunction = .less
        depthDescriptor.isDepthWriteEnabled = true
        guard let depthState = device.makeDepthStencilState(descriptor: depthDescriptor) else {
            return nil
        }
        self.depthStencilState = depthState

        // Create pipeline state
        createPipelineState()
    }

    // MARK: - Pipeline Setup

    private func createPipelineState() {
        guard let library = device.makeDefaultLibrary() else {
            print("Failed to create Metal library")
            return
        }

        // Vertex shader
        let vertexFunction = library.makeFunction(name: "vertex_main")
        // Fragment shader
        let fragmentFunction = library.makeFunction(name: "fragment_main")

        // Pipeline descriptor
        let pipelineDescriptor = MTLRenderPipelineDescriptor()
        pipelineDescriptor.vertexFunction = vertexFunction
        pipelineDescriptor.fragmentFunction = fragmentFunction
        pipelineDescriptor.depthAttachmentPixelFormat = .depth32Float_stencil8
        pipelineDescriptor.colorAttachments[0].pixelFormat = .bgra8Unorm

        // Vertex descriptor
        let vertexDescriptor = MTLVertexDescriptor()

        // Position (0)
        vertexDescriptor.attributes[0].format = .float3
        vertexDescriptor.attributes[0].offset = 0
        vertexDescriptor.attributes[0].bufferIndex = 0

        // Normal (1)
        vertexDescriptor.attributes[1].format = .float3
        vertexDescriptor.attributes[1].offset = MemoryLayout<Float>.size * 3
        vertexDescriptor.attributes[1].bufferIndex = 0

        // Color (2)
        vertexDescriptor.attributes[2].format = .float4
        vertexDescriptor.attributes[2].offset = MemoryLayout<Float>.size * 6
        vertexDescriptor.attributes[2].bufferIndex = 0

        // Layout
        vertexDescriptor.layouts[0].stride = MemoryLayout<Float>.size * 10
        vertexDescriptor.layouts[0].stepFunction = .perVertex

        pipelineDescriptor.vertexDescriptor = vertexDescriptor

        // Create pipeline state
        do {
            pipelineState = try device.makeRenderPipelineState(descriptor: pipelineDescriptor)
        } catch {
            print("Failed to create pipeline state: \(error)")
        }
    }

    // MARK: - Public API (called from Kotlin/Native)

    public func setMesh(vertices: Data, vertexCount: Int, extruderIndices: Data?, extruderCount: Int) {
        self.vertexCount = vertexCount
        self.extruderIndices = extruderIndices
        self.extruderCount = extruderCount

        // Create vertex buffer
        vertexBuffer = device.makeBuffer(bytes: [UInt8](vertices),
                                        length: vertices.count,
                                        options: [])
    }

    public func clearMesh() {
        vertexBuffer = nil
        vertexCount = 0
        extruderIndices = nil
        extruderCount = 0
    }

    public func setInstanceColors(_ colors: [Float], count: Int) {
        instanceColors.removeAll()
        for i in 0..<count {
            let base = i * 4
            let color = SIMD4<Float>(
                colors[base],
                colors[base + 1],
                colors[base + 2],
                colors[base + 3]
            )
            instanceColors.append(color)
        }
    }

    public func setInstancePositions(_ positions: [Float], count: Int) {
        instancePositions.removeAll()
        for i in 0..<count {
            let base = i * 2
            let pos = SIMD2<Float>(positions[base], positions[base + 1])
            instancePositions.append(pos)
        }
    }

    public func setModelScale(x: Float, y: Float, z: Float) {
        modelScale = SIMD3<Float>(x, y, z)
    }

    public func setHighlightIndex(_ index: Int) {
        highlightIndex = index
    }

    public func setWipeTower(x: Float, y: Float, width: Float, depth: Float) {
        wipeTower = (x, y, width, depth)
    }

    public func clearWipeTower() {
        wipeTower = nil
    }

    public func setCameraDistance(_ distance: Float) {
        cameraDistance = distance
    }

    public func setCameraRotation(yaw: Float, pitch: Float) {
        cameraYaw = yaw
        cameraPitch = pitch
    }

    public func resetCamera() {
        cameraDistance = 400
        cameraYaw = 45.0 * (.pi / 180.0)
        cameraPitch = 30.0 * (.pi / 180.0)
    }

    public func render(width: Int, height: Int) {
        guard let drawable = (currentMTKView?.currentDrawable) else { return }
        guard let renderPassDescriptor = (currentMTKView?.currentRenderPassDescriptor) else { return }
        guard let commandBuffer = commandQueue.makeCommandBuffer() else { return }
        guard let renderEncoder = commandBuffer.makeRenderCommandEncoder(descriptor: renderPassDescriptor) else { return }

        // Configure render encoder
        renderEncoder.setRenderPipelineState(pipelineState!)
        renderEncoder.setDepthStencilState(depthStencilState)

        // Set viewport
        renderEncoder.setViewport(MTLViewport(
            originX: 0,
            originY: 0,
            width: Double(width),
            height: Double(height),
            znear: 0.1,
            zfar: 1000
        ))

        // Compute matrices
        let aspect = Float(width) / Float(height)
        let projectionMatrix = matrix_perspective_right_hand(fovyRadians: 45 * (.pi / 180),
                                                             aspectRatio: aspect,
                                                             nearZ: 0.1,
                                                             farZ: 1000)

        let cameraPosition = SIMD3<Float>(
            cameraDistance * sin(cameraYaw) * cos(cameraPitch),
            cameraDistance * sin(cameraPitch),
            cameraDistance * cos(cameraYaw) * cos(cameraPitch)
        )

        let viewMatrix = matrix_look_at_right_hand(eye: cameraPosition,
                                                   center: SIMD3<Float>(0, 0, 0),
                                                   up: SIMD3<Float>(0, 1, 0))

        var modelMatrix = matrix_identity_float4x4
        modelMatrix = matrix_scale(modelMatrix, modelScale.x, modelScale.y, modelScale.z)

        let modelViewProjectionMatrix = projectionMatrix * viewMatrix * modelMatrix

        // Setup uniforms
        var uniforms = Uniforms(
            modelViewProjectionMatrix: modelViewProjectionMatrix,
            modelMatrix: modelMatrix,
            normalMatrix: matrix_float3x3(modelMatrix[0].xyz, modelMatrix[1].xyz, modelMatrix[2].xyz),
            lightDirection: SIMD3<Float>(-0.5, -1.0, -0.5),
            ambientIntensity: 0.3,
            directionalIntensity: 0.7,
            useVertexColor: extruderIndices != nil ? 1 : 0,
            highlightIndex: Int32(highlightIndex)
        )

        // Set uniforms
        renderEncoder.setVertexBytes(&uniforms,
                                    length: MemoryLayout<Uniforms>.stride,
                                    index: 1)

        // Draw mesh
        if let buffer = vertexBuffer, vertexCount > 0 {
            renderEncoder.setVertexBuffer(buffer, offset: 0, index: 0)
            renderEncoder.drawPrimitives(type: .triangle,
                                        vertexStart: 0,
                                        vertexCount: vertexCount)
        }

        renderEncoder.endEncoding()
        commandBuffer.present(drawable)
        commandBuffer.commit()
    }

    public func handleDrag(dx: Float, dy: Float) {
        cameraYaw += dx * 0.01
        cameraPitch += dy * 0.01
        cameraPitch = max(-1.5, min(1.5, cameraPitch))
    }

    public func handleZoom(scale: Float) {
        cameraDistance *= scale
        cameraDistance = max(50, min(1000, cameraDistance))
    }

    public func hitTest(x: Float, y: Float, width: Int, height: Int) -> Int {
        // TODO: Implement ray-based hit testing
        return -1
    }

    // MARK: - Current View Reference

    // This would be set when the renderer is attached to an MTKView
    // For now, we'll store a weak reference
    weak var currentMTKView: MTKView?
}

// ============================================================================
// MARK: - C API for Kotlin/Native Interop
// ============================================================================

/// Opaque pointer to MetalMeshRenderer
private var renderers: [Int: MetalMeshRenderer] = [:]
private var nextRendererID: Int = 1

@_cdecl("metal_renderer_create")
func metal_renderer_create() -> Int {
    guard let renderer = MetalMeshRenderer() else {
        return 0
    }
    let id = nextRendererID
    nextRendererID += 1
    renderers[id] = renderer
    return id
}

@_cdecl("metal_renderer_destroy")
func metal_renderer_destroy(_ rendererID: Int) {
    renderers.removeValue(forKey: rendererID)
}

@_cdecl("metal_renderer_set_mesh")
func metal_renderer_set_mesh(_ rendererID: Int,
                             _ vertices: NSData,
                             _ vertexCount: Int,
                             _ extruderIndices: NSData?,
                             _ extruderCount: Int) {
    guard let renderer = renderers[rendererID] else { return }
    renderer.setMesh(vertices: vertices as Data,
                    vertexCount: vertexCount,
                    extruderIndices: extruderIndices as Data?,
                    extruderCount: extruderCount)
}

@_cdecl("metal_renderer_clear_mesh")
func metal_renderer_clear_mesh(_ rendererID: Int) {
    guard let renderer = renderers[rendererID] else { return }
    renderer.clearMesh()
}

@_cdecl("metal_renderer_set_instance_colors")
func metal_renderer_set_instance_colors(_ rendererID: Int,
                                        _ colors: UnsafePointer<Float>,
                                        _ count: Int) {
    guard let renderer = renderers[rendererID] else { return }
    let colorArray = Array(UnsafeBufferPointer(start: colors, count: count * 4))
    renderer.setInstanceColors(colorArray, count: count)
}

@_cdecl("metal_renderer_set_instance_positions")
func metal_renderer_set_instance_positions(_ rendererID: Int,
                                           _ positions: UnsafePointer<Float>,
                                           _ count: Int) {
    guard let renderer = renderers[rendererID] else { return }
    let posArray = Array(UnsafeBufferPointer(start: positions, count: count * 2))
    renderer.setInstancePositions(posArray, count: count)
}

@_cdecl("metal_renderer_set_model_scale")
func metal_renderer_set_model_scale(_ rendererID: Int, x: Float, y: Float, z: Float) {
    guard let renderer = renderers[rendererID] else { return }
    renderer.setModelScale(x: x, y: y, z: z)
}

@_cdecl("metal_renderer_set_highlight_index")
func metal_renderer_set_highlight_index(_ rendererID: Int, index: Int) {
    guard let renderer = renderers[rendererID] else { return }
    renderer.setHighlightIndex(index)
}

@_cdecl("metal_renderer_set_wipe_tower")
func metal_renderer_set_wipe_tower(_ rendererID: Int, x: Float, y: Float, width: Float, depth: Float) {
    guard let renderer = renderers[rendererID] else { return }
    renderer.setWipeTower(x: x, y: y, width: width, depth: depth)
}

@_cdecl("metal_renderer_clear_wipe_tower")
func metal_renderer_clear_wipe_tower(_ rendererID: Int) {
    guard let renderer = renderers[rendererID] else { return }
    renderer.clearWipeTower()
}

@_cdecl("metal_renderer_set_camera_distance")
func metal_renderer_set_camera_distance(_ rendererID: Int, distance: Float) {
    guard let renderer = renderers[rendererID] else { return }
    renderer.setCameraDistance(distance)
}

@_cdecl("metal_renderer_set_camera_rotation")
func metal_renderer_set_camera_rotation(_ rendererID: Int, yaw: Float, pitch: Float) {
    guard let renderer = renderers[rendererID] else { return }
    renderer.setCameraRotation(yaw: yaw, pitch: pitch)
}

@_cdecl("metal_renderer_reset_camera")
func metal_renderer_reset_camera(_ rendererID: Int) {
    guard let renderer = renderers[rendererID] else { return }
    renderer.resetCamera()
}

@_cdecl("metal_renderer_render")
func metal_renderer_render(_ rendererID: Int, width: Int, height: Int) {
    guard let renderer = renderers[rendererID] else { return }
    renderer.render(width: width, height: height)
}

@_cdecl("metal_renderer_handle_drag")
func metal_renderer_handle_drag(_ rendererID: Int, dx: Float, dy: Float) {
    guard let renderer = renderers[rendererID] else { return }
    renderer.handleDrag(dx: dx, dy: dy)
}

@_cdecl("metal_renderer_handle_zoom")
func metal_renderer_handle_zoom(_ rendererID: Int, scale: Float) {
    guard let renderer = renderers[rendererID] else { return }
    renderer.handleZoom(scale: scale)
}

@_cdecl("metal_renderer_hit_test")
func metal_renderer_hit_test(_ rendererID: Int, x: Float, y: Float, width: Int, height: Int) -> Int {
    guard let renderer = renderers[rendererID] else { return -1 }
    return renderer.hitTest(x: x, y: y, width: width, height: height)
}

// ============================================================================
// MARK: - Matrix Utilities
// ============================================================================

private func matrix_perspective_right_hand(fovyRadians: Float, aspectRatio: Float, nearZ: Float, farZ: Float) -> matrix_float4x4 {
    let ys = 1.0 / tanf(fovyRadians * 0.5)
    let xs = ys / aspectRatio
    let zs = farZ / (nearZ - farZ)
    let wa = -1.0

    return matrix_float4x4(
        SIMD4<Float>(xs, 0, 0, 0),
        SIMD4<Float>(0, ys, 0, 0),
        SIMD4<Float>(0, 0, zs, wa),
        SIMD4<Float>(0, 0, zs * nearZ, 0)
    )
}

private func matrix_look_at_right_hand(eye: SIMD3<Float>, center: SIMD3<Float>, up: SIMD3<Float>) -> matrix_float4x4 {
    let z = normalize(center - eye)
    let x = normalize(cross(up, z))
    let y = cross(z, x)

    return matrix_float4x4(
        SIMD4<Float>(x.x, y.x, z.x, 0),
        SIMD4<Float>(x.y, y.y, z.y, 0),
        SIMD4<Float>(x.z, y.z, z.z, 0),
        SIMD4<Float>(-dot(x, eye), -dot(y, eye), -dot(z, eye), 1)
    )
}

private func matrix_scale(_ matrix: matrix_float4x4, _ x: Float, _ y: Float, _ z: Float) -> matrix_float4x4 {
    var scaled = matrix
    scaled[0][0] *= x
    scaled[1][1] *= y
    scaled[2][2] *= z
    return scaled
}

private func normalize(_ v: SIMD3<Float>) -> SIMD3<Float> {
    let len = sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
    return v / len
}

private func cross(_ a: SIMD3<Float>, _ b: SIMD3<Float>) -> SIMD3<Float> {
    return SIMD3<Float>(
        a.y * b.z - a.z * b.y,
        a.z * b.x - a.x * b.z,
        a.x * b.y - a.y * b.x
    )
}

private func dot(_ a: SIMD3<Float>, _ b: SIMD3<Float>) -> Float {
    return a.x * b.x + a.y * b.y + a.z * b.z
}
