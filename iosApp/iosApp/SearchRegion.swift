import AutkaShared
import SwiftUI

enum SearchRegion: String, CaseIterable, Identifiable {
    case all
    case poland
    case europe
    case usa

    var id: String { rawValue }

    var title: LocalizedStringKey {
        switch self {
        case .all: "All"
        case .poland: "Poland"
        case .europe: "Europe"
        case .usa: "USA"
        }
    }

    var queryValue: String? {
        switch self {
        case .all: nil
        case .poland: "POLAND"
        case .europe: "EUROPE"
        case .usa: "USA"
        }
    }

    var kotlinValues: Set<Region> {
        switch self {
        case .all:
            Set([Region.poland, Region.europe, Region.usa])
        case .poland:
            Set([Region.poland])
        case .europe:
            Set([Region.europe])
        case .usa:
            Set([Region.usa])
        }
    }
}
