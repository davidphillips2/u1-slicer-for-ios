//
//  PrinterScreen.swift
//  U1Slicer
//
//  Printer connection and status screen
//

import SwiftUI

struct PrinterScreen: View {
    @StateObject private var viewModel = PrinterViewModel()
    @State private var showConnectionSheet = false

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    // Connection status
                    ConnectionCard(
                        isConnected: viewModel.isConnected,
                        printerName: viewModel.printerName,
                        onConnect: {
                            showConnectionSheet = true
                        },
                        onDisconnect: {
                            viewModel.disconnect()
                        }
                    )

                    if viewModel.isConnected {
                        // Printer status
                        PrinterStatusCard(status: viewModel.status)

                        // Temperatures
                        TemperatureCard(
                            nozzleTemp: viewModel.nozzleTemp,
                            bedTemp: viewModel.bedTemp,
                            targetNozzle: viewModel.targetNozzleTemp,
                            targetBed: viewModel.targetBedTemp
                        )

                        // Job progress
                        if let progress = viewModel.jobProgress {
                            JobProgressCard(progress: progress)
                        }

                        // Quick actions
                        ActionsCard(
                            onHome: { viewModel.homePrinter() },
                            onExtrude: { viewModel.extrudeFilament() },
                            onPreheat: { viewModel.preheat() }
                        )
                    }
                }
                .padding()
            }
            .navigationTitle("Printer")
            .navigationBarTitleDisplayMode(.large)
            .sheet(isPresented: $showConnectionSheet) {
                PrinterConnectionSheet(
                    onConnect: { ip, port in
                        viewModel.connect(ip: ip, port: port)
                    }
                )
            }
        }
    }
}

// ============================================================================
// MARK: - Connection Card
// ============================================================================

struct ConnectionCard: View {
    let isConnected: Bool
    let printerName: String?
    let onConnect: () -> Void
    let onDisconnect: () -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(isConnected ? "Connected" : "Not Connected")
                    .font(.headline)

                if let name = printerName {
                    Text(name)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }

            Spacer()

            if isConnected {
                Button("Disconnect") {
                    onDisconnect()
                }
                .buttonStyle(.bordered)
            } else {
                Button("Connect") {
                    onConnect()
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding()
        .background(Color(.systemGroupedBackground))
        .cornerRadius(12)
    }
}

// ============================================================================
// MARK: - Printer Status Card
// ============================================================================

struct PrinterStatusCard: View {
    let status: PrinterStatus

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Status")
                .font(.headline)

            HStack {
                Circle()
                    .fill(statusColor)
                    .frame(width: 12, height: 12)

                Text(status.text)
                    .font(.subheadline)

                Spacer()

                if let progress = status.progress {
                    ProgressView(value: progress)
                }
            }
        }
        .padding()
        .background(Color(.systemGroupedBackground))
        .cornerRadius(12)
    }

    private var statusColor: Color {
        switch status.state {
        case .idle, .complete:
            return .green
        case .printing:
            return .blue
        case .paused, .error:
            return .orange
        case .offline:
            return .red
        }
    }
}

// ============================================================================
// MARK: - Temperature Card
// ============================================================================

struct TemperatureCard: View {
    let nozzleTemp: Float
    let bedTemp: Float
    let targetNozzle: Float
    let targetBed: Float

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Temperatures")
                .font(.headline)

            TemperatureRow(label: "Nozzle", current: nozzleTemp, target: targetNozzle)
            TemperatureRow(label: "Bed", current: bedTemp, target: targetBed)
        }
        .padding()
        .background(Color(.systemGroupedBackground))
        .cornerRadius(12)
    }
}

struct TemperatureRow: View {
    let label: String
    let current: Float
    let target: Float

    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)

            Spacer()

            Text(String(format: "%.0f°", current))
                .font(.body)
                .fontWeight(.medium)

            Text("/")
                .foregroundColor(.secondary)

            Text(String(format: "%.0f°", target))
                .font(.body)
                .foregroundColor(.secondary)
        }
    }
}

// ============================================================================
// MARK: - Job Progress Card
// ============================================================================

struct JobProgressCard: View {
    let progress: JobProgress

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Print Progress")
                .font(.headline)

            Text(progress.fileName)
                .font(.subheadline)

            ProgressView(value: progress.progress)

            HStack {
                Text("\(Int(progress.progress * 100))%")
                    .font(.caption)
                    .foregroundColor(.secondary)

                Spacer()

                Text("\(progress.currentLayer) / \(progress.totalLayers)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.systemGroupedBackground))
        .cornerRadius(12)
    }
}

// ============================================================================
// MARK: - Actions Card
// ============================================================================

struct ActionsCard: View {
    let onHome: () -> Void
    let onExtrude: () -> Void
    let onPreheat: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Actions")
                .font(.headline)

            HStack(spacing: 12) {
                ActionButton(title: "Home", icon: "house", action: onHome)
                ActionButton(title: "Extrude", icon: "arrow.right.circle", action: onExtrude)
                ActionButton(title: "Preheat", icon: "flame", action: onPreheat)
            }
        }
        .padding()
        .background(Color(.systemGroupedBackground))
        .cornerRadius(12)
    }
}

struct ActionButton: View {
    let title: String
    let icon: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.title3)

                Text(title)
                    .font(.caption)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(8)
        }
    }
}

// ============================================================================
// MARK: - Printer Connection Sheet
// ============================================================================

struct PrinterConnectionSheet: View {
    @Environment(\.dismiss) var dismiss
    @State private var ipAddress = ""
    @State private var port = "7125"

    let onConnect: (String, String) -> Void

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Moonraker Connection")) {
                    TextField("IP Address", text: $ipAddress)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.numbersAndPunctuation)

                    TextField("Port", text: $port)
                        .keyboardType(.numberPad)
                }
            }
            .navigationTitle("Connect to Printer")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Connect") {
                        onConnect(ipAddress, port)
                        dismiss()
                    }
                    .disabled(ipAddress.isEmpty)
                }
            }
        }
    }
}

// ============================================================================
// MARK: - Printer Status Model
// ============================================================================

struct PrinterStatus {
    let state: PrinterState
    let text: String
    var progress: Float? = nil
}

enum PrinterState {
    case idle
    case printing
    case paused
    case complete
    case error
    case offline
}

struct JobProgress {
    let fileName: String
    let progress: Float
    let currentLayer: Int
    let totalLayers: Int
}

// ============================================================================
// MARK: - Printer View Model (placeholder)
// ============================================================================

class PrinterViewModel: ObservableObject {
    @Published var isConnected = false
    @Published var printerName: String? = nil
    @Published var status = PrinterStatus(state: .offline, text: "Not connected")
    @Published var nozzleTemp: Float = 0
    @Published var bedTemp: Float = 0
    @Published var targetNozzleTemp: Float = 0
    @Published var targetBedTemp: Float = 0
    @Published var jobProgress: JobProgress? = nil

    func connect(ip: String, port: String) {
        // TODO: Implement connection via shared module
        isConnected = true
        printerName = "Snapmaker U1"
        status = PrinterStatus(state: .idle, text: "Idle")
    }

    func disconnect() {
        // TODO: Implement disconnection
        isConnected = false
        printerName = nil
        status = PrinterStatus(state: .offline, text: "Not connected")
    }

    func homePrinter() {
        // TODO: Implement home command
    }

    func extrudeFilament() {
        // TODO: Implement extrude command
    }

    func preheat() {
        // TODO: Implement preheat command
    }
}
