import AutkaShared
import MapKit
import SwiftUI

struct OffersMapView: View {
    private let locatedOffers: [LocatedOffer]
    @State private var region: MKCoordinateRegion
    @State private var selectedOffer: LocatedOffer?

    init(offers: [CarOffer]) {
        let located = offers.compactMap(LocatedOffer.init)
        locatedOffers = located
        _region = State(initialValue: Self.region(for: located))
    }

    var body: some View {
        NavigationStack {
            Group {
                if locatedOffers.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "map")
                            .font(.largeTitle)
                            .foregroundStyle(.secondary)
                        Text("No cars on the map")
                            .font(.headline)
                        Text("The loaded offers do not contain location coordinates.")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding()
                } else {
                    Map(coordinateRegion: $region, annotationItems: locatedOffers) { item in
                        MapAnnotation(coordinate: item.coordinate) {
                            Button {
                                selectedOffer = item
                            } label: {
                                Image(systemName: "car.circle.fill")
                                    .font(.title)
                                    .symbolRenderingMode(.palette)
                                    .foregroundStyle(.white, .tint)
                                    .shadow(radius: 2)
                            }
                            .accessibilityLabel(item.offer.title)
                        }
                    }
                    .ignoresSafeArea(edges: .bottom)
                }
            }
            .navigationTitle("Map")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(item: $selectedOffer) { item in
                NavigationStack {
                    OfferDetailView(offer: item.offer)
                }
            }
        }
    }

    private static func region(for offers: [LocatedOffer]) -> MKCoordinateRegion {
        guard let first = offers.first else {
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: 52.0, longitude: 19.0),
                span: MKCoordinateSpan(latitudeDelta: 8.0, longitudeDelta: 8.0)
            )
        }

        var minLatitude = first.coordinate.latitude
        var maxLatitude = first.coordinate.latitude
        var minLongitude = first.coordinate.longitude
        var maxLongitude = first.coordinate.longitude

        for offer in offers.dropFirst() {
            minLatitude = min(minLatitude, offer.coordinate.latitude)
            maxLatitude = max(maxLatitude, offer.coordinate.latitude)
            minLongitude = min(minLongitude, offer.coordinate.longitude)
            maxLongitude = max(maxLongitude, offer.coordinate.longitude)
        }

        let center = CLLocationCoordinate2D(
            latitude: (minLatitude + maxLatitude) / 2,
            longitude: (minLongitude + maxLongitude) / 2
        )
        let latitudeDelta = max((maxLatitude - minLatitude) * 1.35, 0.25)
        let longitudeDelta = max((maxLongitude - minLongitude) * 1.35, 0.25)
        return MKCoordinateRegion(
            center: center,
            span: MKCoordinateSpan(
                latitudeDelta: min(latitudeDelta, 160),
                longitudeDelta: min(longitudeDelta, 360)
            )
        )
    }
}

private struct LocatedOffer: Identifiable {
    let offer: CarOffer
    let coordinate: CLLocationCoordinate2D

    var id: String { offer.id }

    init?(_ offer: CarOffer) {
        guard let latitude = offer.latitude?.doubleValue,
              let longitude = offer.longitude?.doubleValue,
              (-90...90).contains(latitude),
              (-180...180).contains(longitude) else {
            return nil
        }
        self.offer = offer
        coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}
