import AutkaShared
import SwiftUI

struct ContentView: View {
    @State private var vehiclePriceUsd = 12_000.0
    @State private var shippingUsd = 2_000.0

    private func calculateEstimate() -> ImportCostEstimate {
        ImportCostCalculator.shared.estimate(
            vehiclePriceUsd: vehiclePriceUsd,
            shippingUsd: shippingUsd,
            engineCapacityCc: nil,
            fuelType: FuelType.electric,
            customsDutyRate: 0.10,
            vatRate: 0.23
        )
    }

    var body: some View {
        let estimate = calculateEstimate()

        return NavigationStack {
            Form {
                Section {
                    Label("Shared Kotlin core connected", systemImage: "checkmark.seal.fill")
                } footer: {
                    Text("This native SwiftUI screen calls Autka's shared import-cost logic.")
                }

                Section("Example US import · EV") {
                    Stepper(value: $vehiclePriceUsd, in: 5_000...100_000, step: 1_000) {
                        LabeledContent("Vehicle") {
                            Text(vehiclePriceUsd, format: .currency(code: "USD").precision(.fractionLength(0)))
                        }
                    }

                    Stepper(value: $shippingUsd, in: 500...10_000, step: 250) {
                        LabeledContent("Shipping") {
                            Text(shippingUsd, format: .currency(code: "USD").precision(.fractionLength(0)))
                        }
                    }

                    LabeledContent("Duty") {
                        Text(estimate.customsDuty.amount, format: .currency(code: "USD").precision(.fractionLength(0)))
                    }
                    LabeledContent("VAT") {
                        Text(estimate.vat.amount, format: .currency(code: "USD").precision(.fractionLength(0)))
                    }
                    LabeledContent("Estimated total") {
                        Text(estimate.total.amount, format: .currency(code: "USD").precision(.fractionLength(0)))
                            .fontWeight(.semibold)
                    }
                }

                Section {
                    Text("Indicative estimate only. More of the Android app will move behind this native shell incrementally.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Autka")
        }
    }
}

#Preview {
    ContentView()
}
