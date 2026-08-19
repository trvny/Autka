import AutkaShared
import Foundation
import SwiftUI

enum ListingsSortOption: String, CaseIterable, Identifiable {
    case newest
    case mileage
    case year

    var id: String { rawValue }

    var backendValue: String {
        switch self {
        case .newest: "NEWEST"
        case .mileage: "MILEAGE_ASC"
        case .year: "YEAR_DESC"
        }
    }

    var title: String {
        switch self {
        case .newest: String(localized: "Newest", table: "Filters")
        case .mileage: String(localized: "Lowest mileage", table: "Filters")
        case .year: String(localized: "Newest model year", table: "Filters")
        }
    }
}

struct ListingsFilters: Equatable {
    var minYear: Int?
    var maxYear: Int?
    var maxMileageKm: Int?
    var fuelTypes: Set<String> = []
    var transmissions: Set<String> = []
    var sourceIds: Set<String> = []
    var sort = ListingsSortOption.newest

    var activeCount: Int {
        var count = 0
        if minYear != nil { count += 1 }
        if maxYear != nil { count += 1 }
        if maxMileageKm != nil { count += 1 }
        if !fuelTypes.isEmpty { count += 1 }
        if !transmissions.isEmpty { count += 1 }
        if !sourceIds.isEmpty { count += 1 }
        if sort != .newest { count += 1 }
        return count
    }
}

struct ListingsFiltersView: View {
    private static let fuelChoices = [
        "PETROL",
        "DIESEL",
        "HYBRID",
        "PLUGIN_HYBRID",
        "ELECTRIC",
        "HYDROGEN",
        "LPG",
        "OTHER",
    ]
    private static let transmissionChoices = ["MANUAL", "AUTOMATIC"]

    @Environment(\.dismiss) private var dismiss
    @State private var draft: ListingsFilters
    @State private var minYearText: String
    @State private var maxYearText: String
    @State private var maxMileageText: String

    let availableSources: [SourceHealth]
    let onApply: (ListingsFilters) -> Void

    init(
        filters: ListingsFilters,
        availableSources: [SourceHealth],
        onApply: @escaping (ListingsFilters) -> Void
    ) {
        _draft = State(initialValue: filters)
        _minYearText = State(initialValue: filters.minYear.map(String.init) ?? "")
        _maxYearText = State(initialValue: filters.maxYear.map(String.init) ?? "")
        _maxMileageText = State(initialValue: filters.maxMileageKm.map(String.init) ?? "")
        self.availableSources = availableSources
        self.onApply = onApply
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(String(localized: "Year and mileage", table: "Filters")) {
                    TextField(String(localized: "Minimum year", table: "Filters"), text: $minYearText)
                        .keyboardType(.numberPad)
                    TextField(String(localized: "Maximum year", table: "Filters"), text: $maxYearText)
                        .keyboardType(.numberPad)
                    TextField(String(localized: "Maximum mileage (km)", table: "Filters"), text: $maxMileageText)
                        .keyboardType(.numberPad)
                }

                Section(String(localized: "Fuel", table: "Filters")) {
                    ForEach(Self.fuelChoices, id: \.self) { value in
                        SelectionRow(
                            title: label(for: value),
                            selected: draft.fuelTypes.contains(value)
                        ) {
                            draft.fuelTypes = toggled(value, in: draft.fuelTypes)
                        }
                    }
                }

                Section(String(localized: "Transmission", table: "Filters")) {
                    ForEach(Self.transmissionChoices, id: \.self) { value in
                        SelectionRow(
                            title: label(for: value),
                            selected: draft.transmissions.contains(value)
                        ) {
                            draft.transmissions = toggled(value, in: draft.transmissions)
                        }
                    }
                }

                if !availableSources.isEmpty {
                    Section(String(localized: "Sources")) {
                        ForEach(availableSources, id: \.id) { source in
                            SelectionRow(
                                title: source.displayName,
                                selected: draft.sourceIds.contains(source.id),
                                enabled: source.enabled
                            ) {
                                draft.sourceIds = toggled(source.id, in: draft.sourceIds)
                            }
                        }
                    }
                }

                Section(String(localized: "Sort", table: "Filters")) {
                    Picker(String(localized: "Sort", table: "Filters"), selection: $draft.sort) {
                        ForEach(ListingsSortOption.allCases) { option in
                            Text(option.title).tag(option)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }
            }
            .navigationTitle(String(localized: "Filters", table: "Filters"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "Cancel", table: "Filters")) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .bottomBar) {
                    Button(String(localized: "Reset", table: "Filters")) {
                        reset()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: "Apply", table: "Filters")) {
                        apply()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }

    private func apply() {
        var result = draft
        let parsedMin = parsePositiveInt(minYearText)
        let parsedMax = parsePositiveInt(maxYearText)
        if let parsedMin, let parsedMax, parsedMin > parsedMax {
            result.minYear = parsedMax
            result.maxYear = parsedMin
        } else {
            result.minYear = parsedMin
            result.maxYear = parsedMax
        }
        result.maxMileageKm = parsePositiveInt(maxMileageText)
        onApply(result)
        dismiss()
    }

    private func reset() {
        draft = ListingsFilters()
        minYearText = ""
        maxYearText = ""
        maxMileageText = ""
    }

    private func parsePositiveInt(_ value: String) -> Int? {
        guard let parsed = Int(value), parsed > 0 else { return nil }
        return parsed
    }

    private func toggled(_ value: String, in set: Set<String>) -> Set<String> {
        var result = set
        if !result.insert(value).inserted {
            result.remove(value)
        }
        return result
    }

    private func label(for value: String) -> String {
        switch value {
        case "PETROL": String(localized: "Petrol")
        case "DIESEL": String(localized: "Diesel")
        case "HYBRID": String(localized: "Hybrid")
        case "PLUGIN_HYBRID": String(localized: "Plug-in hybrid")
        case "ELECTRIC": String(localized: "Electric")
        case "HYDROGEN": String(localized: "Hydrogen")
        case "LPG": String(localized: "LPG")
        case "OTHER": String(localized: "Other")
        case "MANUAL": String(localized: "Manual", table: "Filters")
        case "AUTOMATIC": String(localized: "Automatic", table: "Filters")
        default: value
        }
    }
}

private struct SelectionRow: View {
    let title: String
    let selected: Bool
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(title)
                    .foregroundStyle(enabled ? .primary : .secondary)
                Spacer()
                if selected {
                    Image(systemName: "checkmark")
                        .fontWeight(.semibold)
                }
            }
        }
        .disabled(!enabled)
    }
}
