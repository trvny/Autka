import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            MarketplaceSearchView()
                .tabItem {
                    Label("Search", systemImage: "magnifyingglass")
                }

            ImportCalculatorView()
                .tabItem {
                    Label("Import", systemImage: "shippingbox")
                }
        }
    }
}

#Preview {
    ContentView()
}
