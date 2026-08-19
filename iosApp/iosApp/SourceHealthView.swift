import AutkaShared
import Foundation
import SwiftUI

struct SourceHealthView: View {
    @State private var sources: [SourceHealth] = []
    @State private var isLoading = false
    @State private var loadFailed = false
    @State private var hasLoaded = false

    var body: some View {
        NavigationStack {
            List {
                if isLoading && sources.isEmpty {
                    Section {
                        HStack {
                            Spacer()
                            ProgressView()
                            Spacer()
                        }
                    }
                }

                if loadFailed {
                    Section {
                        Text("Source diagnostics could not be refreshed.")
                            .foregroundStyle(.red)
                        Button("Retry") {
                            Task { await refresh() }
                        }
                        .disabled(isLoading)
                    }
                }

                if !isLoading && !loadFailed && sources.isEmpty {
                    Section {
                        Text("No source diagnostics are available.")
                            .foregroundStyle(.secondary)
                    }
                }

                ForEach(sources, id: \.id) { source in
                    SourceHealthRow(source: source)
                }
            }
            .navigationTitle("Sources")
            .refreshable {
                await refresh()
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        Task { await refresh() }
                    } label: {
                        if isLoading && !sources.isEmpty {
                            ProgressView()
                        } else {
                            Image(systemName: "arrow.clockwise")
                        }
                    }
                    .disabled(isLoading)
                    .accessibilityLabel("Refresh")
                }
            }
            .task {
                guard !hasLoaded else { return }
                hasLoaded = true
                await refresh()
            }
        }
    }

    @MainActor
    private func refresh() async {
        guard !isLoading else { return }
        isLoading = true
        loadFailed = false
        defer { isLoading = false }

        guard let decoded = await SourceCatalog.fetch() else {
            loadFailed = true
            return
        }
        sources = decoded.sortedForDisplay
    }
}

private struct SourceHealthRow: View {
    let source: SourceHealth

    var body: some View {
        Section {
            LabeledContent("Status") {
                Label(status.title, systemImage: status.systemImage)
                    .foregroundStyle(status.color)
            }

            if source.displayName != source.id {
                LabeledContent("Source ID", value: source.id)
            }

            if let offerCount = source.offerCount {
                LabeledContent("Offers", value: Int(offerCount.intValue).formatted())
            }

            if let completedAt = source.lastCompletedAtEpochMs {
                let date = Date(timeIntervalSince1970: Double(completedAt.int64Value) / 1_000.0)
                LabeledContent("Last completed") {
                    Text(date.formatted(date: .abbreviated, time: .shortened))
                }
            }

            if let upserted = source.lastOffersUpserted {
                LabeledContent("Offers in last run", value: Int(upserted.intValue).formatted())
            }
        } header: {
            Text(source.displayName)
        }
    }

    private var status: SourceStatus {
        if !source.enabled { return .disabled }
        if source.offerCount == nil { return .unavailable }
        if source.lastCompletedOk?.boolValue == true { return .healthy }
        if source.lastCompletedOk?.boolValue == false { return .failed }
        return .noRun
    }
}

private enum SourceStatus {
    case disabled
    case unavailable
    case healthy
    case failed
    case noRun

    var title: LocalizedStringKey {
        switch self {
        case .disabled: "Disabled"
        case .unavailable: "Unavailable"
        case .healthy: "Healthy"
        case .failed: "Last run failed"
        case .noRun: "No completed run"
        }
    }

    var systemImage: String {
        switch self {
        case .disabled: "pause.circle"
        case .unavailable, .failed: "exclamationmark.triangle"
        case .healthy: "checkmark.circle"
        case .noRun: "clock"
        }
    }

    var color: Color {
        switch self {
        case .healthy: .green
        case .unavailable, .failed: .red
        case .disabled, .noRun: .secondary
        }
    }
}

private extension Array where Element == SourceHealth {
    var sortedForDisplay: [SourceHealth] {
        sorted { lhs, rhs in
            if lhs.enabled != rhs.enabled {
                return lhs.enabled && !rhs.enabled
            }
            return lhs.id < rhs.id
        }
    }
}

#Preview {
    SourceHealthView()
}
