//
//  PrepareScreen.swift
//  U1Slicer
//
//  Main prepare screen for loading models and configuring slicing
//

import SwiftUI

struct PrepareScreen: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var viewModel = PrepareViewModel()

    @State private var showFilePicker = false
    @State private var showSlicingProgress = false
    @State private var showSliceComplete = false
    @State private var showShareSheet = false

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Model viewer (3D preview)
                if let meshData = viewModel.meshData {
                    Model3DView(meshData: meshData, extruderColors: viewModel.extruderColors)
                        .frame(height: 300)
                        .background(Color.black.opacity(0.1))
                } else {
                    EmptyModelView()
                        .frame(height: 300)
                        .onTapGesture {
                            showFilePicker = true
                        }
                }

                Divider()

                // Slicing configuration
                ScrollView {
                    VStack(spacing: 16) {
                        // Model info
                        if let modelInfo = viewModel.modelInfo {
                            ModelInfoCard(modelInfo: modelInfo)
                        }

                        // Quick settings
                        QuickSettingsCard(config: $viewModel.config)

                        // Advanced settings
                        AdvancedSettingsCard(config: $viewModel.config)

                        // Slice button
                        SliceButton(
                            hasModel: viewModel.meshData != nil,
                            isSlicing: viewModel.isSlicing,
                            onTap: {
                                startSlicing()
                            }
                        )
                        .padding(.bottom, 16)
                    }
                    .padding()
                }
            }
            .navigationTitle("Prepare")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button("Load Model") {
                            showFilePicker = true
                        }

                        if viewModel.meshData != nil {
                            Button("Clear Model") {
                                viewModel.clearModel()
                            }

                            Button("Export G-code") {
                                exportGcode()
                            }
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            .sheet(isPresented: $showFilePicker) {
                DocumentPicker(fileTypes: ["stl", "3mf", "obj", "step"]) { url in
                    viewModel.loadModel(from: url)
                }
            }
        }
    }

    private func startSlicing() {
        guard let meshData = viewModel.meshData else { return }

        viewModel.slice { progress in
            appState.slicingProgress = progress
        } onComplete: { result in
            if result.success {
                appState.currentJob = SliceJobInfo(
                    id: Int(result.jobId),
                    modelName: viewModel.modelInfo?.filename ?? "Unknown",
                    gcodePath: result.gcodePath ?? "",
                    totalLayers: result.totalLayers,
                    estimatedTime: result.estimatedTimeSeconds,
                    timestamp: Date()
                )
                showSliceComplete = true
            }
        }
    }

    private func exportGcode() {
        showShareSheet = true
    }
}

// ============================================================================
// MARK: - Empty Model View
// ============================================================================

struct EmptyModelView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "cube.badge.plus")
                .font(.system(size: 60))
                .foregroundColor(.gray)

            Text("No Model Loaded")
                .font(.headline)
                .foregroundColor(.primary)

            Text("Tap to load a 3D model file")
                .font(.subheadline)
                .foregroundColor(.secondary)

            Text("Supported: STL, 3MF, OBJ, STEP")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .background(Color.gray.opacity(0.1))
    }
}

// ============================================================================
// MARK: - Model Info Card
// ============================================================================

struct ModelInfoCard: View {
    let modelInfo: ModelInfo

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Model Information")
                .font(.headline)

            HStack {
                InfoItem(label: "File", value: modelInfo.filename)
                Spacer()
                InfoItem(label: "Format", value: modelInfo.format.uppercased())
            }

            HStack {
                InfoItem(label: "Size", value: String(format: "%.1f × %.1f × %.1f mm",
                    modelInfo.sizeX, modelInfo.sizeY, modelInfo.sizeZ))
                Spacer()
                InfoItem(label: "Triangles", value: "\(modelInfo.triangleCount)")
            }
        }
        .padding()
        .background(Color(.systemGroupedBackground))
        .cornerRadius(12)
    }
}

struct InfoItem: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label.uppercased())
                .font(.caption)
                .foregroundColor(.secondary)
            Text(value)
                .font(.subheadline)
                .fontWeight(.medium)
        }
    }
}

// ============================================================================
// MARK: - Quick Settings Card
// ============================================================================

struct QuickSettingsCard: View {
    @Binding var config: SliceConfig

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Quick Settings")
                .font(.headline)

            HStack {
                SettingSlider(
                    label: "Layer Height",
                    value: $config.layerHeight,
                    range: 0.1...0.4,
                    step: 0.05,
                    suffix: "mm"
                )

                Divider()
                    .frame(height: 60)

                SettingSlider(
                    label: "Infill",
                    value: Binding(
                        get: { config.fillDensity },
                        set: { config.fillDensity = $0 }
                    ),
                    range: 0...1,
                    step: 0.05,
                    suffix: "%",
                    multiplier: 100
                )
            }
        }
        .padding()
        .background(Color(.systemGroupedBackground))
        .cornerRadius(12)
    }
}

struct SettingSlider: View {
    let label: String
    @Binding var value: Float
    let range: ClosedRange<Float>
    let step: Float
    let suffix: String
    var multiplier: Float = 1.0

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.subheadline)

            HStack {
                Slider(
                    value: $value,
                    in: range,
                    step: step
                )

                Text(String(format: "%.2f%@", value * multiplier, suffix))
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .frame(width: 50)
            }
        }
    }
}

// ============================================================================
// MARK: - Advanced Settings Card
// ============================================================================

struct AdvancedSettingsCard: View {
    @Binding var config: SliceConfig
    @State private var isExpanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Button(action: { isExpanded.toggle() }) {
                HStack {
                    Text("Advanced Settings")
                        .font(.headline)
                        .foregroundColor(.primary)

                    Spacer()

                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(.secondary)
                }
            }

            if isExpanded {
                VStack(spacing: 8) {
                    TemperatureRow(config: $config)
                    SpeedRow(config: $config)
                    SupportRow(config: $config)
                }
            }
        }
        .padding()
        .background(Color(.systemGroupedBackground))
        .cornerRadius(12)
    }
}

struct TemperatureRow: View {
    @Binding var config: SliceConfig

    var body: some View {
        HStack {
            Text("Temperature")
            Spacer()
            Text("\(config.nozzleTemp)°C / \(config.bedTemp)°C")
                .foregroundColor(.secondary)
        }
    }
}

struct SpeedRow: View {
    @Binding var config: SliceConfig

    var body: some View {
        HStack {
            Text("Print Speed")
            Spacer()
            Text("\(Int(config.printSpeed)) mm/s")
                .foregroundColor(.secondary)
        }
    }
}

struct SupportRow: View {
    @Binding var config: SliceConfig

    var body: some View {
        HStack {
            Text("Support")
            Spacer()
            Text(config.supportEnabled ? "Enabled" : "Disabled")
                .foregroundColor(.secondary)

            Toggle("", isOn: $config.supportEnabled)
                .labelsHidden()
        }
    }
}

// ============================================================================
// MARK: - Slice Button
// ============================================================================

struct SliceButton: View {
    let hasModel: Bool
    let isSlicing: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                if isSlicing {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle())
                        .foregroundColor(.white)
                } else {
                    Image(systemName: "play.circle.fill")
                        .font(.title2)
                }

                Text(isSlicing ? "Slicing..." : "Slice Model")
                    .font(.headline)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(hasModel ? Color.blue : Color.gray)
            .foregroundColor(.white)
            .cornerRadius(12)
        }
        .disabled(!hasModel || isSlicing)
    }
}

// ============================================================================
// MARK: - Document Picker
// ============================================================================

struct DocumentPicker: UIViewControllerRepresentable {
    let fileTypes: [String]
    let onPick: (URL) -> Void

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let picker = UIDocumentPickerViewController(
            documentTypes: fileTypes.map { "public.$0-content" },
            in: .import
        )
        picker.delegate = context.coordinator
        picker.allowsMultipleSelection = false
        return picker
    }

    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(onPick: onPick)
    }

    class Coordinator: NSObject, UIDocumentPickerDelegate {
        let onPick: (URL) -> Void

        init(onPick: @escaping (URL) -> Void) {
            self.onPick = onPick
        }

        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            guard let url = urls.first else { return }
            onPick(url)
        }
    }
}

// ============================================================================
// MARK: - Slice Config Model (placeholder)
// ============================================================================

struct SliceConfig {
    var layerHeight: Float = 0.2
    var fillDensity: Float = 0.15
    var nozzleTemp: Int = 210
    var bedTemp: Int = 60
    var printSpeed: Float = 60
    var supportEnabled: Bool = false

    // Add more settings as needed
}

// ============================================================================
// MARK: - Model Info Model (placeholder)
// ============================================================================

struct ModelInfo {
    let filename: String
    let format: String
    let sizeX: Float
    let sizeY: Float
    let sizeZ: Float
    let triangleCount: Int
}
