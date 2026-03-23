//
//  SettingsScreen.swift
//  U1Slicer
//
//  App settings screen
//

import SwiftUI

struct SettingsScreen: View {
    @StateObject private var viewModel = SettingsViewModel()

    var body: some View {
        NavigationView {
            List {
                // App settings
                Section("App Settings") {
                    Toggle("Dark Mode", isOn: $viewModel.darkMode)

                    NavigationLink {
                        FilamentSettingsView()
                    } label: {
                        Label("Filament Profiles", systemImage: "paintpalette")
                    }

                    NavigationLink {
                        SlicerSettingsView()
                    } label: {
                        Label("Slicer Settings", systemImage: "slider.horizontal.3")
                    }
                }

                // Printer settings
                Section("Printer") {
                    NavigationLink {
                        PrinterSettingsView()
                    } label: {
                        Label("Printer Profiles", systemImage: "printer")
                    }

                    NavigationLink {
                        ConnectionSettingsView()
                    } label: {
                        Label("Connection Settings", systemImage: "network")
                    }
                }

                // About
                Section("About") {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text(viewModel.appVersion)
                            .foregroundColor(.secondary)
                    }

                    Link(destination: URL(string: "https://github.com/davidphillips2/u1-slicer-for-ios")!) {
                        HStack {
                            Text("Source Code")
                            Spacer()
                            Image(systemName: "arrow.up.right.square")
                                .foregroundColor(.secondary)
                        }
                    }

                    Link(destination: URL(string: "https://github.com/davidphillips2/u1-slicer-for-ios/issues")!) {
                        HStack {
                            Text("Report Issue")
                            Spacer()
                            Image(systemName: "arrow.up.right.square")
                                .foregroundColor(.secondary)
                        }
                    }

                    Link(destination: URL(string: "https://snapmaker.com")!) {
                        HStack {
                            Text("Snapmaker U1 Support")
                            Spacer()
                            Image(systemName: "arrow.up.right.square")
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.large)
        }
    }
}

// ============================================================================
// MARK: - Settings Views
// ============================================================================

struct FilamentSettingsView: View {
    var body: some View {
        List {
            Text("Filament profiles management coming soon")
                .foregroundColor(.secondary)
        }
        .navigationTitle("Filament Profiles")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct SlicerSettingsView: View {
    var body: some View {
        List {
            Text("Slicer settings coming soon")
                .foregroundColor(.secondary)
        }
        .navigationTitle("Slicer Settings")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct PrinterSettingsView: View {
    var body: some View {
        List {
            Text("Printer profiles coming soon")
                .foregroundColor(.secondary)
        }
        .navigationTitle("Printer Profiles")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct ConnectionSettingsView: View {
    var body: some View {
        List {
            Text("Connection settings coming soon")
                .foregroundColor(.secondary)
        }
        .navigationTitle("Connection Settings")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// ============================================================================
// MARK: - Settings View Model
// ============================================================================

class SettingsViewModel: ObservableObject {
    @Published var darkMode = false
    let appVersion = "1.0.0 (iOS KMP)"
}
