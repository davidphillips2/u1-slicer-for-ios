//
//  U1SlicerApp.swift
//  U1Slicer
//
//  Main app entry point for U1 Slicer iOS
//  SwiftUI app with tab-based navigation
//

import SwiftUI

@main
struct U1SlicerApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .onAppear {
                    setupApp()
                }
        }
    }

    private func setupApp() {
        // Initialize app
        print("U1 Slicer iOS starting...")
        print("Kotlin Multiplatform shared module: loaded")
    }
}

// ============================================================================
// MARK: - App State Management
// ============================================================================

/// Global app state shared across all screens
class AppState: ObservableObject {
    // Current selected tab
    @Published var selectedTab: Tab = .prepare

    // Navigation paths
    @Published var navigationPath = NavigationPath()

    // Slicer state
    @Published var isSlicing = false
    @Published var slicingProgress: Float = 0.0
    @Published var slicingStage: String = ""

    // Current job (for preview)
    @Published var currentJob: SliceJobInfo?

    // Show settings
    @Published var showSettings = false
}

// ============================================================================
// MARK: - Tabs
// ============================================================================

enum Tab: String, CaseIterable {
    case prepare = "Prepare"
    case preview = "Preview"
    case jobs = "Jobs"
    case printer = "Printer"
    case settings = "Settings"

    var icon: String {
        switch self {
        case .prepare: return "cube"
        case .preview: return "eye"
        case .jobs: return "folder"
        case .printer: return "printer"
        case .settings: return "gearshape"
        }
    }
}

// ============================================================================
// MARK: - Main Content View
// ============================================================================

struct ContentView: View {
    @EnvironmentObject var appState: AppState
    @State private var selectedTab: Tab = .prepare

    var body: some View {
        TabView(selection: $selectedTab) {
            PrepareScreen()
                .tabItem {
                    Label("Prepare", systemImage: Tab.prepare.icon)
                }
                .tag(Tab.prepare)

            PreviewScreen()
                .tabItem {
                    Label("Preview", systemImage: Tab.preview.icon)
                }
                .tag(Tab.preview)

            JobsScreen()
                .tabItem {
                    Label("Jobs", systemImage: Tab.jobs.icon)
                }
                .tag(Tab.jobs)

            PrinterScreen()
                .tabItem {
                    Label("Printer", systemImage: Tab.printer.icon)
                }
                .tag(Tab.printer)

            SettingsScreen()
                .tabItem {
                    Label("Settings", systemImage: Tab.settings.icon)
                }
                .tag(Tab.settings)
        }
        .tint(.blue)
    }
}

// ============================================================================
// MARK: - Slice Job Info (for navigation)
// ============================================================================

struct SliceJobInfo: Identifiable {
    let id: Int
    let modelName: String
    let gcodePath: String
    let totalLayers: Int
    let estimatedTime: Float
    let timestamp: Date
}
