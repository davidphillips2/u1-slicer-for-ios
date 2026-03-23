//
//  PrepareViewModel.swift
//  U1Slicer
//
//  View model for Prepare screen
//

import SwiftUI
import UniformTypeIdentifiers

class PrepareViewModel: ObservableObject {
    @Published var meshData: MeshData3D? = nil
    @Published var modelInfo: ModelInfo3D? = nil
    @Published var isSlicing = false
    @Published var slicingProgress: Float = 0.0
    @Published var slicingStage: String = ""

    let extruderColors: [Color] = [
        .red,      // E1
        .green,    // E2
        .blue,     // E3
        .yellow    // E4
    ]

    var config: SliceConfig3D = SliceConfig3D()

    // MARK: - Model Loading

    func loadModel(from url: URL) {
        // TODO: Load model via shared module's parser
        // For now, create placeholder data
        loadPlaceholderModel(filename: url.lastPathComponent)
    }

    private func loadPlaceholderModel(filename: String = "model.stl") {
        // Create a simple cube as placeholder
        let vertices: [Float] = [
            // Front face
            -1, -1,  1,  0,  0,  1,  1,  0,  0,  1,
             1, -1,  1,  0,  0,  1,  1,  0,  0,  1,
             1,  1,  1,  0,  0,  1,  1,  0,  0,  1,
            -1,  1,  1,  0,  0,  1,  1,  0,  0,  1,
        ]

        meshData = MeshData3D(
            vertices: vertices,
            vertexCount: 4,
            extruderIndices: [0]
        )

        modelInfo = ModelInfo3D(
            filename: filename,
            format: "stl",
            sizeX: 20.0,
            sizeY: 20.0,
            sizeZ: 20.0,
            triangleCount: 2
        )
    }

    func clearModel() {
        meshData = nil
        modelInfo = nil
    }

    // MARK: - Slicing

    func slice(progress: @escaping (Float) -> Void, onComplete: @escaping (SliceResult3D) -> Void) {
        guard meshData != nil else { return }

        isSlicing = true
        slicingProgress = 0.0

        // Simulate slicing progress
        Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { timer in
            self.slicingProgress += 0.05

            if self.slicingProgress >= 1.0 {
                timer.invalidate()
                self.isSlicing = false

                let result = SliceResult3D(
                    success: true,
                    jobId: 1,
                    gcodePath: "/path/to/output.gcode",
                    totalLayers: 100,
                    estimatedTimeSeconds: 3600
                )

                onComplete(result)
            } else {
                progress(self.slicingProgress)
            }
        }
    }
}

// ============================================================================
// MARK: - Models
// ============================================================================

struct ModelInfo3D {
    let filename: String
    let format: String
    let sizeX: Float
    let sizeY: Float
    let sizeZ: Float
    let triangleCount: Int
}

struct SliceConfig3D {
    var layerHeight: Float = 0.2
    var fillDensity: Float = 0.15
    var nozzleTemp: Int = 210
    var bedTemp: Int = 60
    var printSpeed: Float = 60
    var supportEnabled: Bool = false
}

struct SliceResult3D {
    let success: Bool
    var errorMessage: String? = nil
    var jobId: Int64 = 0
    var gcodePath: String? = nil
    var totalLayers: Int = 0
    var estimatedTimeSeconds: Float = 0
}
