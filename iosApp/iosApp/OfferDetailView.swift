import AutkaShared
import Foundation
import SwiftUI

struct OfferDetailView: View {
    let offer: CarOffer

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 18) {
                gallery

                VStack(alignment: .leading, spacing: 8) {
                    Text(offer.title)
                        .font(.title2.bold())

                    Text(
                        offer.price.amount,
                        format: .currency(code: offer.price.currency.name).precision(.fractionLength(0))
                    )
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.tint)
                }

                actions
                specifications
            }
            .padding(.bottom, 24)
        }
        .navigationTitle("Offer")
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private var gallery: some View {
        let images = offer.imageUrls.isEmpty
            ? [offer.thumbnailUrl].compactMap { $0 }
            : offer.imageUrls

        if images.isEmpty {
            OfferImageView(urlString: nil)
                .frame(height: 230)
                .clipped()
        } else {
            ScrollView(.horizontal) {
                LazyHStack(spacing: 10) {
                    ForEach(Array(images.enumerated()), id: \.offset) { _, url in
                        OfferImageView(urlString: url)
                            .frame(width: 320, height: 220)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                }
                .scrollTargetLayout()
                .padding(.horizontal)
            }
            .scrollTargetBehavior(.viewAligned)
            .scrollIndicators(.hidden)
        }
    }

    @ViewBuilder
    private var actions: some View {
        if let url = URL(string: offer.listingUrl) {
            HStack(spacing: 12) {
                Link(destination: url) {
                    Label("Open listing", systemImage: "arrow.up.right")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)

                ShareLink(item: url, subject: Text(offer.title)) {
                    Label("Share", systemImage: "square.and.arrow.up")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
            .padding(.horizontal)
        }
    }

    private var specifications: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Details")
                .font(.headline)
                .padding(.bottom, 8)

            DetailRow("Make", offer.make)
            DetailRow("Model", offer.model)
            DetailRow("Year", offer.year.map { String($0.intValue) })
            DetailRow("Mileage", offer.mileageKm.map { "\(Int($0.intValue).formatted()) km" })
            DetailRow("Fuel", fuelLabel)
            DetailRow("Transmission", transmissionLabel)
            DetailRow("Power", offer.powerHp.map { "\($0.intValue) hp" })
            DetailRow("Location", offer.location)
            DetailRow("Region", regionLabel)
            DetailRow("Source", offer.sourceId)

            if let listingCount = offer.listingCount?.intValue, listingCount > 1 {
                DetailRow("Marketplaces", String(listingCount))
            }

            if let postedAt = offer.postedAtEpochMs {
                let date = Date(timeIntervalSince1970: Double(postedAt.int64Value) / 1_000.0)
                DetailRow("Posted", date.formatted(date: .abbreviated, time: .shortened))
            }
        }
        .padding(.horizontal)
    }

    private var fuelLabel: LocalizedStringKey {
        switch offer.fuelType.name {
        case "PETROL": "Petrol"
        case "DIESEL": "Diesel"
        case "HYBRID": "Hybrid"
        case "PLUGIN_HYBRID": "Plug-in hybrid"
        case "ELECTRIC": "Electric"
        case "HYDROGEN": "Hydrogen"
        case "LPG": "LPG"
        case "OTHER": "Other"
        default: "Unknown"
        }
    }

    private var transmissionLabel: LocalizedStringKey {
        switch offer.transmission.name {
        case "MANUAL": "Manual"
        case "AUTOMATIC": "Automatic"
        default: "Unknown"
        }
    }

    private var regionLabel: LocalizedStringKey {
        switch offer.region.name {
        case "POLAND": "Poland"
        case "EUROPE": "Europe"
        case "USA": "USA"
        default: "Unknown"
        }
    }
}

private struct DetailRow: View {
    let label: LocalizedStringKey
    let value: String?

    init(_ label: LocalizedStringKey, _ value: String?) {
        self.label = label
        self.value = value
    }

    var body: some View {
        if let value, !value.isEmpty {
            LabeledContent(label) {
                Text(value)
                    .multilineTextAlignment(.trailing)
            }
            .padding(.vertical, 7)
            Divider()
        }
    }
}
