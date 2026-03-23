//
//  PreviewScreen.swift
//  U1Slicer
//
//  G-code preview screen with layer-by-layer visualization
//

import SwiftUI

struct PreviewScreen: View {
    @EnvironmentObject var appState: AppState
    @State private var currentLayer = 0
    @State private var totalLayers = 100
    @State private var showLayerInfo = false

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                if let job = appState.currentJob {
                    // Layer preview view
                    LayerPreviewView(
                        gcodePath: job.gcodePath,
                        currentLayer: $currentLayer,
                        totalLayers: job.totalLayers
                    )
                    .frame(height: 400)
                    .background(Color.black)

                    Divider()

                    // Layer controls
                    VStack(spacing: 16) {
                        // Layer slider
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text("Layer")
                                    .font(.headline)
                                Spacer()
                                Text("\(currentLayer + 1) / \(job.totalLayers)")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }

                            Slider(
                                value: Binding(
                                    get: { Float(currentLayer) },
                                    set: { currentLayer = Int($0) }
                                ),
                                in: 0...Float(job.totalLayers - 1),
                                step: 1
                            )
                        }

                        // Job info
                        JobInfoCard(job: job)

                        // Action buttons
                        HStack(spacing: 16) {
                            Button("Export G-code") {
                                exportGcode()
                            }
                            .buttonStyle(.borderedProminent)

                            Button("Send to Printer") {
                                sendToPrinter()
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                    .padding()
                } else {
                    // Empty state
                    VStack(spacing: 16) {
                        Image(systemName: "eye.slash")
                            .font(.system(size: 60))
                            .foregroundColor(.gray)

                        Text("No G-code to Preview")
                            .font(.headline)

                        Text("Slice a model first to see the preview")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            .navigationTitle("Preview")
            .navigationBarTitleDisplayMode(.large)
        }
    }

    private func exportGcode() {
        // Export G-code functionality
    }

    private func sendToPrinter() {
        // Send to printer functionality
    }
}

// ============================================================================
// MARK: - Layer Preview View
// ============================================================================

struct LayerPreviewView: View {
    let gcodePath: String
    @Binding var currentLayer: Int
    let totalLayers: Int

    var body: some View {
        VStack {
            // Placeholder for actual layer preview
            ZStack {
                Color.black

                VStack(spacing: 8) {
                    Image(systemName: "layer")
                        .font(.system(size: 40))
                        .foregroundColor(.blue)

                    Text("Layer \(currentLayer + 1) of \(totalLayers)")
                        .foregroundColor(.white)

                    Text("G-code preview coming soon")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
    }
}

// ============================================================================
// MARK: - Job Info Card
// ============================================================================

struct JobInfoCard: View {
    let job: SliceJobInfo

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Job Information")
                .font(.headline)

            InfoRow(label: "Model", value: job.modelName)
            InfoRow(label: "Layers", value: "\(job.totalLayers)")
            InfoRow(label: "Est. Time", value: formatTime(job.estimatedTime))
            InfoRow(label: "Date", value: formatDate(job.timestamp))
        }
        .padding()
        .background(Color(.systemGroupedBackground))
        .cornerRadius(12)
    }

    private func formatTime(_ seconds: Float) -> String {
        let hours = Int(seconds) / 3600
        let minutes = Int(seconds) % 3600 / 60

        if hours > 0 {
            return "\(hours)h \(minutes)m"
        } else {
            return "\(minutes)m"
        }
    }

    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}

struct InfoRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .fontWeight(.medium)
        }
    }
}
