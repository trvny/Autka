import AutkaShared
import SwiftUI

struct MarketplaceSearchView: View {
    @State private var make = ""
    @State private var model = ""
    @State private var region = SearchRegion.poland
    @State private var links: [MarketplaceLink] = []

    private func refreshLinks() {
        let filter = SearchFilter(
            query: "",
            make: make.isEmpty ? nil : make,
            model: model.isEmpty ? nil : model,
            minPrice: nil,
            maxPrice: nil,
            minYear: nil,
            maxYear: nil,
            maxMileageKm: nil,
            fuelTypes: Set<FuelType>(),
            transmissions: Set<Transmission>(),
            regions: Set<Region>([region.kotlinValue]),
            sourceIds: Set<String>(),
            sort: SortOrder.newest
        )
        links = MarketplaceSearchLinks.shared.all(filter: filter, affiliateId: nil)
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    TextField("Make", text: $make)
                        .textInputAutocapitalization(.words)
                        .autocorrectionDisabled()

                    TextField("Model", text: $model)
                        .textInputAutocapitalization(.words)
                        .autocorrectionDisabled()

                    Picker("Region", selection: $region) {
                        ForEach(SearchRegion.allCases) { choice in
                            Text(choice.title).tag(choice)
                        }
                    }
                    .pickerStyle(.segmented)
                } footer: {
                    Text("Autka prepares the search, then opens the original marketplace. No listing data is copied or scraped.")
                }

                Section("Marketplaces") {
                    ForEach(links, id: \.sourceId) { link in
                        if let url = URL(string: link.url) {
                            Link(destination: url) {
                                HStack {
                                    Text(link.displayName)
                                    Spacer()
                                    Image(systemName: "arrow.up.right")
                                        .foregroundStyle(.secondary)
                                }
                            }
                        } else {
                            HStack {
                                Text(link.displayName)
                                Spacer()
                                Image(systemName: "exclamationmark.triangle")
                                    .foregroundStyle(.secondary)
                                    .accessibilityLabel("Invalid link")
                            }
                            .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Search cars")
            .onAppear(perform: refreshLinks)
            .onChange(of: make) { _ in refreshLinks() }
            .onChange(of: model) { _ in refreshLinks() }
            .onChange(of: region) { _ in refreshLinks() }
        }
    }
}

private enum SearchRegion: String, CaseIterable, Identifiable {
    case poland
    case europe
    case usa

    var id: String { rawValue }

    var title: LocalizedStringKey {
        switch self {
        case .poland: "Poland"
        case .europe: "Europe"
        case .usa: "USA"
        }
    }

    var kotlinValue: Region {
        switch self {
        case .poland: Region.poland
        case .europe: Region.europe
        case .usa: Region.usa
        }
    }
}

#Preview {
    MarketplaceSearchView()
}
