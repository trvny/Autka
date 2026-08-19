import AutkaShared
import Foundation
import SwiftUI

struct ListingsView: View {
    private static let pageSize = 50

    @State private var query = ""
    @State private var region = SearchRegion.all
    @State private var offers: [CarOffer] = []
    @State private var isLoading = false
    @State private var loadFailed = false
    @State private var reachedEnd = false
    @State private var nextOffset = 0
    @State private var requestGeneration = 0
    @State private var hasLoaded = false
    @State private var showMarketplaces = false
    @State private var showMap = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Picker("Region", selection: $region) {
                        ForEach(SearchRegion.allCases) { choice in
                            Text(choice.title).tag(choice)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                if loadFailed {
                    Section {
                        Text("Cars could not be refreshed.")
                            .foregroundStyle(.red)
                        Button("Retry") {
                            Task { await refresh() }
                        }
                        .disabled(isLoading)
                    }
                }

                if isLoading && offers.isEmpty {
                    Section {
                        HStack {
                            Spacer()
                            ProgressView("Loading cars…")
                            Spacer()
                        }
                    }
                } else if offers.isEmpty && !loadFailed {
                    Section {
                        Text("No cars match this search yet.")
                            .foregroundStyle(.secondary)
                        Button("Search marketplaces") {
                            showMarketplaces = true
                        }
                    }
                } else {
                    Section {
                        ForEach(offers, id: \.id) { offer in
                            NavigationLink {
                                OfferDetailView(offer: offer)
                            } label: {
                                OfferRow(offer: offer)
                            }
                        }

                        if !reachedEnd {
                            Button {
                                Task { await loadNextPage() }
                            } label: {
                                HStack {
                                    Spacer()
                                    if isLoading {
                                        ProgressView()
                                    } else {
                                        Text("Load more")
                                    }
                                    Spacer()
                                }
                            }
                            .disabled(isLoading)
                        }
                    }
                }
            }
            .navigationTitle("Autka")
            .searchable(text: $query, prompt: "Search cars")
            .onSubmit(of: .search) {
                Task { await refresh() }
            }
            .onChange(of: query) { _ in
                requestGeneration += 1
                isLoading = false
                loadFailed = false
            }
            .onChange(of: region) { _ in
                Task { await refresh() }
            }
            .refreshable {
                await refresh()
            }
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button {
                        showMap = true
                    } label: {
                        Image(systemName: "map")
                    }
                    .disabled(offers.isEmpty)
                    .accessibilityLabel("Map")

                    Button {
                        showMarketplaces = true
                    } label: {
                        Image(systemName: "arrow.up.right.square")
                    }
                    .accessibilityLabel("Search marketplaces")
                }
            }
            .sheet(isPresented: $showMap) {
                OffersMapView(offers: offers)
            }
            .sheet(isPresented: $showMarketplaces) {
                MarketplaceSearchView()
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
        requestGeneration += 1
        let generation = requestGeneration
        isLoading = false
        nextOffset = 0
        reachedEnd = false
        await loadPage(offset: 0, generation: generation, replacing: true)
    }

    @MainActor
    private func loadNextPage() async {
        guard !isLoading, !reachedEnd else { return }
        await loadPage(offset: nextOffset, generation: requestGeneration, replacing: false)
    }

    @MainActor
    private func loadPage(offset: Int, generation: Int, replacing: Bool) async {
        guard !isLoading else { return }
        isLoading = true
        loadFailed = false
        defer {
            if requestGeneration == generation {
                isLoading = false
            }
        }

        guard let url = offersURL(offset: offset) else {
            loadFailed = true
            return
        }

        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            guard requestGeneration == generation,
                  let httpResponse = response as? HTTPURLResponse,
                  (200...299).contains(httpResponse.statusCode),
                  let payload = String(data: data, encoding: .utf8),
                  let decoded = OfferDecoder.shared.decodeJsonOrNull(payload: payload) else {
                if requestGeneration == generation {
                    loadFailed = true
                }
                return
            }

            if replacing {
                offers = decoded
            } else {
                let existingIds = Set(offers.map(\.id))
                offers.append(contentsOf: decoded.filter { !existingIds.contains($0.id) })
            }
            nextOffset = offset + decoded.count
            reachedEnd = decoded.count < Self.pageSize
        } catch {
            if requestGeneration == generation {
                loadFailed = true
            }
        }
    }

    private func offersURL(offset: Int) -> URL? {
        var components = URLComponents(
            url: AppConfiguration.backendBaseURL.appendingPathComponent("offers"),
            resolvingAgainstBaseURL: false
        )
        var items = [
            URLQueryItem(name: "sort", value: "NEWEST"),
            URLQueryItem(name: "limit", value: String(Self.pageSize)),
            URLQueryItem(name: "offset", value: String(offset)),
        ]
        let trimmedQuery = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmedQuery.isEmpty {
            items.append(URLQueryItem(name: "query", value: trimmedQuery))
        }
        if let region = region.queryValue {
            items.append(URLQueryItem(name: "regions", value: region))
        }
        components?.queryItems = items
        return components?.url
    }
}

private struct OfferRow: View {
    let offer: CarOffer

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            OfferImageView(urlString: offer.thumbnailUrl)
                .frame(width: 96, height: 76)
                .clipShape(RoundedRectangle(cornerRadius: 10))

            VStack(alignment: .leading, spacing: 5) {
                Text(offer.title)
                    .font(.headline)
                    .lineLimit(2)

                Text(
                    offer.price.amount,
                    format: .currency(code: offer.price.currency.name).precision(.fractionLength(0))
                )
                .font(.subheadline.weight(.semibold))

                Text(metadata)
                    .font(.footnote)
                    .foregroundStyle(.secondary)

                HStack(spacing: 8) {
                    if let location = offer.location, !location.isEmpty {
                        Text(location)
                            .lineLimit(1)
                    }
                    Text(regionBadge)
                        .font(.caption2.weight(.semibold))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(.secondary.opacity(0.15), in: Capsule())
                }
                .font(.footnote)
                .foregroundStyle(.secondary)

                if let listingCount = offer.listingCount?.intValue, listingCount > 1 {
                    HStack(spacing: 3) {
                        Text(verbatim: "\(listingCount)×")
                        Text("Marketplaces")
                    }
                    .font(.caption)
                    .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
    }

    private var metadata: String {
        let year = offer.year.map { String($0.intValue) } ?? "—"
        let mileage = offer.mileageKm.map { "\(Int($0.intValue).formatted()) km" } ?? "—"
        return "\(year) · \(mileage)"
    }

    private var regionBadge: String {
        switch offer.region.name {
        case "POLAND": "PL"
        case "EUROPE": "EU"
        case "USA": "USA"
        default: offer.region.name
        }
    }
}

#Preview {
    ListingsView()
}
