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
                .padding(.horizontal)

                actions
                specifications
            }
            .padding(.bottom, 24)
        }
        .navigationTitle(String(localized: "Offer", table: "OfferDetail"))
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
                .padding(.horizontal)
            }
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
                    Label(String(localized: "Share", table: "OfferDetail"), systemImage: "square.and.arrow.up")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
            .padding(.horizontal)
        }
    }

    private var specifications: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(String(localized: "Details", table: "OfferDetail"))
                .font(.headline)
                .padding(.bottom, 8)

            DetailRow(String(localized: "Make"), offer.make)
            DetailRow(String(localized: "Model"), offer.model)
            DetailRow(String(localized: "Year"), offer.year.map { String($0.intValue) })
            DetailRow(
                String(localized: "Mileage", table: "OfferDetail"),
                offer.mileageKm.map { "\(Int($0.intValue).formatted()) km" }
            )
            DetailRow(String(localized: "Fuel"), fuelLabel)
            DetailRow(String(localized: "Transmission"), transmissionLabel)
            DetailRow(String(localized: "Power"), offer.powerHp.map { "\($0.intValue) hp" })
            DetailRow(String(localized: "Location", table: "OfferDetail"), offer.location)
            DetailRow(String(localized: "Region"), regionLabel)
            DetailRow(String(localized: "Source", table: "OfferDetail"), offer.sourceId)

            if let listingCount = offer.listingCount?.intValue, listingCount > 1 {
                DetailRow(String(localized: "Marketplaces"), String(listingCount))
            }

            if let postedAt = offer.postedAtEpochMs {
                let date = Date(timeIntervalSince1970: Double(postedAt.int64Value) / 1_000.0)
                DetailRow(
                    String(localized: "Posted", table: "OfferDetail"),
                    date.formatted(date: .abbreviated, time: .shortened)
                )
            }
        }
        .padding(.horizontal)
    }

    private var fuelLabel: String {
        switch offer.fuelType.name {
        case "PETROL": String(localized: "Petrol")
        case "DIESEL": String(localized: "Diesel")
        case "HYBRID": String(localized: "Hybrid")
        case "PLUGIN_HYBRID": String(localized: "Plug-in hybrid")
        case "ELECTRIC": String(localized: "Electric")
        case "HYDROGEN": String(localized: "Hydrogen")
        case "LPG": String(localized: "LPG")
        case "OTHER": String(localized: "Other")
        default: String(localized: "Unknown")
        }
    }

    private var transmissionLabel: String {
        switch offer.transmission.name {
        case "MANUAL": String(localized: "Manual", table: "OfferDetail")
        case "AUTOMATIC": String(localized: "Automatic", table: "OfferDetail")
        default: String(localized: "Unknown")
        }
    }

    private var regionLabel: String {
        switch offer.region.name {
        case "POLAND": String(localized: "Poland")
        case "EUROPE": String(localized: "Europe")
        case "USA": String(localized: "USA")
        default: String(localized: "Unknown")
        }
    }
}

private struct DetailRow: View {
    let label: String
    let value: String?

    init(_ label: String, _ value: String?) {
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
