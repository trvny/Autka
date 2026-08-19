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
    var make = ""
    var model = ""
    var minYear: Int?
    var maxYear: Int?
    var maxMileageKm: Int?
    var fuelTypes: Set<String> = []
    var transmissions: Set<String> = []
    var sort = ListingsSortOption.newest

    var activeCount: Int {
        var count = 0
        if !make.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { count += 1 }
        if !model.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { count += 1 }
        if minYear != nil { count += 1 }
        if maxYear != nil { count += 1 }
        if maxMileageKm != nil { count += 1 }
        if !fuelTypes.isEmpty { count += 1 }
        if !transmissions.isEmpty { count += 1 }
        if sort != .newest { count += 1 }
        return count
    }
}

struct ListingsFiltersView: View {
    private static let fuelChoices: [(String, String)] = [
        ("PETROL", "Petrol"),
        ("DIESEL", "Diesel"),
        ("HYBRID", "Hybrid"),
        ("PLUGIN_HYBRID", "Plug-in hybrid"),
        ("ELECTRIC", "Electric"),
        ("HYDROGEN", "Hydrogen"),
        ("LPG", "LPG"),
        ("OTHER", "Other"),
    ]
    private static let transmissionChoices: [(String, String)] = [
        ("MANUAL", "Manual"),
        ("AUTOMATIC", "Automatic"),
    ]

    @Environment(\.dismiss) private var dismiss
    @State private var draft: ListingsFilters
    @State private var minYearText: String
    @State private var maxYearText: String
    @State private var maxMileageText: String

    let onApply: (ListingsFilters) -> Void

    init(filters: ListingsFilters, onApply: @escaping (ListingsFilters) -> Void) {
        _draft = State(initialValue: filters)
        _minYearText = State(initialValue: filters.minYear.map(String.init) ?? "")
        _maxYearText = State(initialValue: filters.maxYear.map(String.init) ?? "")
        _maxMileageText = State(initialValue: filters.maxMileageKm.map(String.init) ?? "")
        self.onApply = onApply
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(String(localized: "Vehicle", table: "Filters")) {
                    TextField(String(localized: "Make"), text: $draft.make)
                        .textInputAutocapitalization(.words)
                        .autocorrectionDisabled()
                    TextField(String(localized: "Model"), text: $draft.model)
                        .textInputAutocapitalization(.words)
                        .autocorrectionDisabled()
                }

                Section(String(localized: "Year and mileage", table: "Filters")) {
                    TextField(String(localized: "Minimum year", table: "Filters"), text: $minYearText)
                        .keyboardType(.numberPad)
                    TextField(String(localized: "Maximum year", table: "Filters"), text: $maxYearText)
                        .keyboardType(.numberPad)
                    TextField(String(localized: "Maximum mileage (km)", table: "Filters"), text: $maxMileageText)
                        .keyboardType(.numberPad)
                }

                Section(String(localized: "Fuel", table: "Filters")) {
                    ForEach(Self.fuelChoices, id: \.0) { value, key in
                        SelectionRow(
                            title: String(localized: String.LocalizationValue(key)),
                            selected: draft.fuelTypes.contains(value)
                        ) {
                            toggle(value, in: &draft.fuelTypes)
                        }
                    }
                }

                Section(String(localized: "Transmission", table: "Filters")) {
                    ForEach(Self.transmissionChoices, id: \.0) { value, key in
                        SelectionRow(
                            title: String(localized: String.LocalizationValue(key), table: "Filters"),
                            selected: draft.transmissions.contains(value)
                        ) {
                            toggle(value, in: &draft.transmissions)
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
        result.make = result.make.trimmingCharacters(in: .whitespacesAndNewlines)
        result.model = result.model.trimmingCharacters(in: .whitespacesAndNewlines)
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

    private func toggle(_ value: String, in set: inout Set<String>) {
        if set.contains(value) {
            set.remove(value)
        } else {
            set.insert(value)
        }
    }
}

private struct SelectionRow: View {
    let title: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(title)
                    .foregroundStyle(.primary)
                Spacer()
                if selected {
                    Image(systemName: "checkmark")
                        .fontWeight(.semibold)
                }
            }
        }
    }
}
