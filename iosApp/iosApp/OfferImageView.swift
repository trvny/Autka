import SwiftUI

struct OfferImageView: View {
    let urlString: String?

    var body: some View {
        AsyncImage(url: urlString.flatMap(URL.init(string:))) { phase in
            switch phase {
            case .success(let image):
                image
                    .resizable()
                    .scaledToFill()
            default:
                ZStack {
                    Color.secondary.opacity(0.12)
                    Image(systemName: "car.side")
                        .font(.title2)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }
}
