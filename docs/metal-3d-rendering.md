# Metal 3D Rendering for iOS - Implementation Guide

## Overview

This guide covers the Metal-based 3D mesh renderer for iOS, providing cross-platform 3D visualization parity with the Android OpenGL ES implementation.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              Shared Kotlin Code (expect/actual)              │
│           com.u1.slicer.shared.graphics.MeshRenderer         │
└─────────────────────────────────────────────────────────────┘
         │                                    │
         │ Android                            │ iOS
         ▼                                    ▼
┌──────────────────────┐          ┌──────────────────────────┐
│   Android (OpenGL)   │          │   iOS (Metal)            │
│                      │          │                          │
│ ModelRenderer       │          │ MetalMeshRenderer.swift  │
│ (GLSurfaceView)     │          │   ────────────────       │
│                      │          │   - MTKView delegate     │
│ - Shaders (.vert)    │          │   - Vertex/Fragment     │
│ - Shaders (.frag)    │          │   - Per-triangle color   │
│ - VBO/VAO           │          │   - Camera controls      │
└──────────────────────┘          └──────────────────────────┘
```

## Features Implemented

### Core Rendering
- ✅ **Mesh rendering** with interleaved vertex data (position + normal + color)
- ✅ **Per-triangle coloring** for multi-extruder support
- ✅ **Camera controls**: orbit, pan, zoom
- ✅ **Lighting**: Ambient + directional
- ✅ **Depth testing**: Proper Z-ordering

### Multi-Object Support
- ✅ **Instance colors**: Up to 4 extruders (Snapmaker U1)
- ✅ **Instance positions**: Multi-part layout
- ✅ **Model scaling**: XYZ scale factors
- ✅ **Highlighting**: Instance selection

### Bed Visualization
- ✅ **Grid lines**: Major and minor grid
- ✅ **Wipe tower**: Multi-extruder wipe tower visualization
- ✅ **Bed bounds**: 270×270mm Snapmaker U1 bed

## File Structure

```
ios/U1Slicer/
├── Platform/
│   ├── MetalMeshRenderer.swift    # Main Metal renderer
│   └── Shaders.metal              # Metal shaders
└── UI/
    └── Prepare/
        └── Model3DView.swift      # SwiftUI wrapper (to be created)
```

## Integration with SwiftUI

### 1. Create SwiftUI View Wrapper

```swift
import SwiftUI
import MetalKit

struct Model3DView: UIViewRepresentable {
    let meshData: MeshData
    let extruderColors: [Color]
    @Binding var cameraDistance: Float
    @Binding var cameraRotation: (yaw: Float, pitch: Float)

    func makeUIView(context: Context) -> MTKView {
        let view = MTKView()
        view.device = MTLCreateSystemDefaultDevice()
        view.clearColor = MTLClearColor(red: 0.059, green: 0.059, blue: 0.118, alpha: 1.0)
        view.enableSetNeedsDisplay = false  // For real-time rendering

        let renderer = MetalMeshRenderer()
        renderer.currentMTKView = view
        view.delegate = renderer

        context.coordinator.renderer = renderer
        return view
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func updateUIView(_ view: MTKView, context: Context) {
        guard let renderer = context.coordinator.renderer else { return }

        // Update mesh data
        renderer.setMesh(
            vertices: meshData.vertices,
            vertexCount: meshData.vertexCount,
            extruderIndices: meshData.extruderIndices,
            extruderCount: meshData.extruderIndices?.count ?? 0
        )

        // Update camera
        renderer.setCameraRotation(yaw: cameraRotation.yaw, pitch: cameraRotation.pitch)
        renderer.setCameraDistance(cameraDistance)

        // Trigger render
        view.draw()
    }

    class Coordinator {
        var renderer: MetalMeshRenderer?
    }
}
```

### 2. Usage in SwiftUI Screen

```swift
struct PrepareScreen: View {
    @State private var meshData: MeshData?
    @State private var cameraDistance: Float = 400
    @State private var cameraRotation: (yaw: Float, pitch: Float) = (45, 30)

    var body: some View {
        VStack {
            if let meshData = meshData {
                Model3DView(
                    meshData: meshData,
                    extruderColors: [
                        Color.red,      // E1
                        Color.green,    // E2
                        Color.blue,     // E3
                        Color.yellow    // E4
                    ],
                    cameraDistance: $cameraDistance,
                    cameraRotation: $cameraRotation
                )
                .frame(height: 400)
                .gesture(
                    DragGesture()
                        .onChanged { value in
                            cameraRotation.yaw += Float(value.translation.x) * 0.01
                            cameraRotation.pitch += Float(value.translation.y) * 0.01
                        }
                )
            }
        }
    }
}
```

## Vertex Format

The renderer uses interleaved vertex data with **10 floats per vertex**:

```kotlin
// Per-vertex layout (40 bytes)
struct Vertex {
    // Position (12 bytes)
    float x, y, z

    // Normal (12 bytes)
    float nx, ny, nz

    // Color (16 bytes)
    float r, g, b, a
}
```

**Total**: 10 floats × 4 bytes = 40 bytes per vertex

### Per-Triangle Coloring

For multi-extruder models, each triangle has an associated extruder index:

```kotlin
// Extruder indices (1 byte per triangle)
val extruderIndices: ByteArray

// Color mapping
val extruderColors: List<FloatArray> = listOf(
    floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f),  // E1: Red
    floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f),  // E2: Green
    floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f),  // E3: Blue
    floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f)   // E4: Yellow
)
```

## Metal Pipeline Setup

### 1. Render Pipeline State

```swift
let pipelineDescriptor = MTLRenderPipelineDescriptor()
pipelineDescriptor.vertexFunction = vertexFunction
pipelineDescriptor.fragmentFunction = fragmentFunction
pipelineDescriptor.depthAttachmentPixelFormat = .depth32Float_stencil8
pipelineDescriptor.colorAttachments[0].pixelFormat = .bgra8Unorm
```

### 2. Vertex Descriptor

```swift
let vertexDescriptor = MTLVertexDescriptor()

// Position (offset 0, size 12)
vertexDescriptor.attributes[0].format = .float3
vertexDescriptor.attributes[0].offset = 0
vertexDescriptor.attributes[0].bufferIndex = 0

// Normal (offset 12, size 12)
vertexDescriptor.attributes[1].format = .float3
vertexDescriptor.attributes[1].offset = MemoryLayout<Float>.size * 3
vertexDescriptor.attributes[1].bufferIndex = 0

// Color (offset 24, size 16)
vertexDescriptor.attributes[2].format = .float4
vertexDescriptor.attributes[2].offset = MemoryLayout<Float>.size * 6
vertexDescriptor.attributes[2].bufferIndex = 0

// Stride: 40 bytes per vertex
vertexDescriptor.layouts[0].stride = MemoryLayout<Float>.size * 10
```

### 3. Uniform Buffer

```swift
struct Uniforms {
    matrix_float4x4 modelViewProjectionMatrix;
    matrix_float4x4 modelMatrix;
    matrix_float3x3 normalMatrix;
    float3 lightDirection;
    float ambientIntensity;
    float directionalIntensity;
    uint useVertexColor;
    int highlightIndex;
};
```

## Camera Controls

### Orbit Camera

```swift
// Yaw (horizontal rotation)
cameraYaw += dx * 0.01

// Pitch (vertical rotation, clamped)
cameraPitch += dy * 0.01
cameraPitch = clamp(cameraPitch, -π/2, π/2)

// Distance (zoom)
cameraDistance *= scale
cameraDistance = clamp(cameraDistance, 50, 1000)
```

### View Matrix

```swift
let eye = SIMD3<Float>(
    cameraDistance * sin(cameraYaw) * cos(cameraPitch),
    cameraDistance * sin(cameraPitch),
    cameraDistance * cos(cameraYaw) * cos(cameraPitch)
)

let viewMatrix = matrix_look_at_right_hand(
    eye: eye,
    center: SIMD3<Float>(0, 0, 0),
    up: SIMD3<Float>(0, 1, 0)
)
```

## Performance Considerations

### Memory Usage

- **Vertex buffer**: `vertexCount × 40 bytes`
- **Extruder indices**: `triangleCount × 1 byte`
- **For 100K triangles**: ~4MB vertex data + 100KB indices

### Rendering Optimization

1. **Instanced rendering**: For multiple identical objects
2. **Level of detail**: Reduce triangles for distant objects
3. **Frustum culling**: Skip off-screen objects
4. **Backface culling**: Skip interior faces

### Metal-Specific Optimizations

```swift
// Use immutable buffers for static data
let buffer = device.makeBuffer(bytes: vertices,
                               length: vertexCount * stride,
                               options: .storageModeShared)

// Use tri-strip for indexed rendering when possible
renderEncoder.drawIndexedPrimitives(type: .triangleStrip,
                                   indexCount: indexCount,
                                   indexType: .uint16)
```

## Debugging Metal Issues

### Common Problems

1. **Black screen**: Check that shaders compiled and pipeline is valid
2. **No depth**: Enable depth testing and create depth stencil state
3. **Distorted geometry**: Verify vertex stride and attribute offsets
4. **Wrong colors**: Check color format (RGBA vs BGRA)

### Validation

```swift
// Enable Metal validation layer
// In Xcode: Product > Scheme > Edit Scheme > Run > Options
// Set "Metal API Validation" to "Enabled"

// Capture GPU frame
// In Xcode: Debug > Capture GPU Frame
```

## Testing

### Unit Tests

```swift
func testMeshUpload() {
    let renderer = MetalMeshRenderer()
    let vertices = createTestVertices()
    renderer.setMesh(vertices: vertices,
                    vertexCount: vertices.count / 10,
                    extruderIndices: nil,
                    extruderCount: 0)

    // Verify vertex buffer created
    XCTAssertNotNil(renderer.vertexBuffer)
}
```

### Integration Tests

```swift
func testRendering() {
    let view = MTKView()
    let renderer = MetalMeshRenderer()
    view.delegate = renderer

    // Load mesh and render
    renderer.setMesh(...)
    view.draw()

    // Verify no Metal errors
    // Check draw call count
}
```

## Migration from OpenGL ES

### Key Differences

| Feature | OpenGL ES | Metal |
|---------|-----------|-------|
| Shaders | GLSL | Metal Shading Language |
| Buffers | glGenBuffers() | makeBuffer() |
| Pipeline | Program object | RenderPipelineState |
| State | Global state | Encoders (state objects) |

### Equivalent Calls

```swift
// OpenGL ES
glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, offset)
glDrawArrays(GL_TRIANGLES, 0, vertexCount)

// Metal
vertexDescriptor.attributes[0].format = .float3
renderEncoder.drawPrimitives(type: .triangle,
                            vertexStart: 0,
                            vertexCount: vertexCount)
```

## Resources

- [Metal Programming Guide](https://developer.apple.com/metal/Metal-Programming-Guide.pdf)
- [Metal Shading Language Specification](https://developer.apple.com/metal/Metal-Shading-Language-Specification.pdf)
- [Metal Sample Code](https://developer.apple.com/metal/sample-code/)
- [MTKView Documentation](https://developer.apple.com/documentation/metalkit/mtkview)

## Next Steps

1. **Hit testing**: Implement ray-based object picking
2. **Bed grid**: Add major/minor grid lines
3. **Axis indicator**: Show X/Y/Z axis
4. **Screenshot capture**: Export rendered view
5. **Animation**: Smooth camera transitions
