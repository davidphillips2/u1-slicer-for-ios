//
//  JobsScreen.swift
//  U1Slicer
//
//  Slice job history screen
//

import SwiftUI

struct JobsScreen: View {
    @StateObject private var viewModel = JobsViewModel()
    @State private var showDeleteAlert = false
    @State private var jobToDelete: SliceJobItem?

    var body: some View {
        NavigationView {
            Group {
                if viewModel.jobs.isEmpty {
                    EmptyJobsView()
                } else {
                    List {
                        ForEach(viewModel.jobs) { job in
                            JobRow(job: job)
                                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                    Button(role: .destructive) {
                                        jobToDelete = job
                                        showDeleteAlert = true
                                    } label: {
                                        Label("Delete", systemImage: "trash")
                                    }
                                }
                        }
                    }
                    .listStyle(.insetGrouped)
                    .refreshable {
                        viewModel.loadJobs()
                    }
                }
            }
            .navigationTitle("Slice Jobs")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    if !viewModel.jobs.isEmpty {
                        Button("Clear All") {
                            viewModel.clearAllJobs()
                        }
                    }
                }
            }
            .alert("Delete Job", isPresented: $showDeleteAlert) {
                Button("Cancel", role: .cancel) {}
                Button("Delete", role: .destructive) {
                    if let job = jobToDelete {
                        viewModel.deleteJob(job)
                    }
                }
            } message: {
                Text("Are you sure you want to delete this slice job?")
            }
        }
        .onAppear {
            viewModel.loadJobs()
        }
    }
}

// ============================================================================
// MARK: - Empty Jobs View
// ============================================================================

struct EmptyJobsView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "folder.badge.questionmark")
                .font(.system(size: 60))
                .foregroundColor(.gray)

            Text("No Slice Jobs Yet")
                .font(.headline)

            Text("Slice a model to see it here")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// ============================================================================
// MARK: - Job Row
// ============================================================================

struct JobRow: View {
    let job: SliceJobItem

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(job.modelName)
                    .font(.headline)

                Spacer()

                Text(formatDate(job.timestamp))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            HStack {
                Label("\(job.totalLayers) layers", systemImage: "layers")
                    .font(.caption)
                    .foregroundColor(.secondary)

                Spacer()

                Label(formatTime(job.estimatedTime), systemImage: "clock")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }

    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        return formatter.string(from: date)
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
}

// ============================================================================
// MARK: - Slice Job Item
// ============================================================================

struct SliceJobItem: Identifiable {
    let id: Int
    let modelName: String
    let gcodePath: String
    let totalLayers: Int
    let estimatedTime: Float
    let timestamp: Date
}

// ============================================================================
// MARK: - Jobs View Model (placeholder)
// ============================================================================

class JobsViewModel: ObservableObject {
    @Published var jobs: [SliceJobItem] = []

    func loadJobs() {
        // TODO: Load from database via shared module
        jobs = []
    }

    func deleteJob(_ job: SliceJobItem) {
        // TODO: Delete from database
        jobs.removeAll { $0.id == job.id }
    }

    func clearAllJobs() {
        // TODO: Clear all jobs from database
        jobs = []
    }
}
