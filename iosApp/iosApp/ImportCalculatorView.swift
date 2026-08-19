import AutkaShared
import SwiftUI

struct ImportCalculatorView: View {
    @State private var vehiclePriceUsd = 20_000.0
    @State private var shippingUsd = 2_400.0
    @State private var fuel = FuelChoice.petrol
    @State private var engineCapacityKnown = true
    @State private var engineCapacityCc = 2_000
    @State private var customsPercent = 10.0
    @State private var vatPercent = 23.0

    private var engineRequired: Bool {
        !fuel.engineExempt
    }

    private func calculateEstimate() -> ImportCostEstimate {
        let engineCapacity: KotlinInt? = engineRequired && engineCapacityKnown
            ? KotlinInt(value: Int32(engineCapacityCc))
            : nil

        return ImportCostCalculator.shared.estimate(
            vehiclePriceUsd: vehiclePriceUsd,
            shippingUsd: shippingUsd,
            engineCapacityCc: engineCapacity,
            fuelType: fuel.kotlinValue,
            customsDutyRate: customsPercent / 100.0,
            vatRate: vatPercent / 100.0
        )
    }

    private func percent(_ value: Double) -> String {
        value.formatted(.number.precision(.fractionLength(0...1))) + "%"
    }

    var body: some View {
        let estimate = calculateEstimate()

        NavigationStack {
            Form {
                Section {
                    LabeledContent("Vehicle price") {
                        Text(vehiclePriceUsd, format: .currency(code: "USD").precision(.fractionLength(0)))
                    }
                    Stepper("Adjust vehicle price", value: $vehiclePriceUsd, in: 1_000...200_000, step: 500)
                        .labelsHidden()

                    LabeledContent("Shipping") {
                        Text(shippingUsd, format: .currency(code: "USD").precision(.fractionLength(0)))
                    }
                    Stepper("Adjust shipping", value: $shippingUsd, in: 0...20_000, step: 100)
                        .labelsHidden()

                    Picker("Drivetrain", selection: $fuel) {
                        ForEach(FuelChoice.allCases) { choice in
                            Text(choice.title).tag(choice)
                        }
                    }
                    .pickerStyle(.menu)

                    if engineRequired {
                        Toggle("Engine capacity known", isOn: $engineCapacityKnown)

                        if engineCapacityKnown {
                            Stepper(value: $engineCapacityCc, in: 500...8_000, step: 100) {
                                LabeledContent("Engine") {
                                    Text(engineCapacityCc.formatted()) + Text(verbatim: " cm³")
                                }
                            }
                        }
                    }
                } header: {
                    Text("Vehicle")
                } footer: {
                    Text("USD values are used for the landed-cost estimate.")
                }

                Section("Import assumptions") {
                    Stepper(value: $customsPercent, in: 0...30, step: 0.5) {
                        LabeledContent("Customs duty") {
                            Text(percent(customsPercent))
                        }
                    }

                    Stepper(value: $vatPercent, in: 0...30, step: 0.5) {
                        LabeledContent("VAT") {
                            Text(percent(vatPercent))
                        }
                    }
                }

                Section("Estimated landed cost") {
                    CostRow(title: "Vehicle", money: estimate.vehiclePrice)
                    CostRow(title: "Shipping", money: estimate.shipping)
                    CostRow(title: "Customs duty", money: estimate.customsDuty)
                    CostRow(title: "Excise", money: estimate.exciseDuty)
                    CostRow(title: "VAT", money: estimate.vat)

                    LabeledContent("Total") {
                        Text(
                            estimate.total.amount,
                            format: .currency(code: estimate.total.currency.name).precision(.fractionLength(0))
                        )
                        .fontWeight(.bold)
                    }

                    if estimate.usesConservativeExcise {
                        Label(
                            "Unknown engine capacity uses the conservative excise rate.",
                            systemImage: "exclamationmark.triangle.fill"
                        )
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    }
                }

                Section {
                    Text("Indicative estimate only. Customs valuation, classification, transport and applicable tax relief can change the final cost.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Import calculator")
        }
    }
}

private struct CostRow: View {
    let title: LocalizedStringKey
    let money: Money

    var body: some View {
        LabeledContent(title) {
            Text(
                money.amount,
                format: .currency(code: money.currency.name).precision(.fractionLength(0))
            )
        }
    }
}

private enum FuelChoice: String, CaseIterable, Identifiable {
    case petrol
    case diesel
    case hybrid
    case pluginHybrid
    case electric
    case hydrogen
    case lpg
    case other
    case unknown

    var id: String { rawValue }

    var title: LocalizedStringKey {
        switch self {
        case .petrol: "Petrol"
        case .diesel: "Diesel"
        case .hybrid: "Hybrid"
        case .pluginHybrid: "Plug-in hybrid"
        case .electric: "Electric"
        case .hydrogen: "Hydrogen"
        case .lpg: "LPG"
        case .other: "Other"
        case .unknown: "Unknown"
        }
    }

    var kotlinValue: FuelType {
        switch self {
        case .petrol: FuelType.petrol
        case .diesel: FuelType.diesel
        case .hybrid: FuelType.hybrid
        case .pluginHybrid: FuelType.pluginHybrid
        case .electric: FuelType.electric
        case .hydrogen: FuelType.hydrogen
        case .lpg: FuelType.lpg
        case .other: FuelType.other
        case .unknown: FuelType.unknown
        }
    }

    var engineExempt: Bool {
        self == .electric || self == .hydrogen
    }
}

#Preview {
    ImportCalculatorView()
}
