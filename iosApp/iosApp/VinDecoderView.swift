import AutkaShared
import SwiftUI

struct VinDecoderView: View {
    @State private var vin = ""
    @State private var isLoading = false
    @State private var validationError = false
    @State private var loadFailed = false
    @State private var result: VinDecodeResult?
    @State private var requestGeneration = 0

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("VIN", text: $vin)
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                        .onChange(of: vin) { value in
                            let normalized = VinInput.shared.normalize(value: value)
                            if normalized != value {
                                vin = normalized
                            }
                            requestGeneration += 1
                            validationError = false
                            loadFailed = false
                            result = nil
                        }
                        .onSubmit {
                            startDecode()
                        }

                    if validationError {
                        Text("Enter a valid 17-character VIN without I, O or Q.")
                            .font(.footnote)
                            .foregroundStyle(.red)
                    } else {
                        Text("17 characters. Letters I, O and Q are not valid VIN characters.")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }

                    Button("Decode VIN") {
                        startDecode()
                    }
                    .disabled(isLoading)
                }

                if isLoading {
                    Section {
                        ProgressView("Decoding VIN…")
                    }
                }

                if loadFailed {
                    Section {
                        Text("VIN decoding failed. Check your connection and try again.")
                            .foregroundStyle(.red)
                        Button("Retry") {
                            startDecode()
                        }
                        .disabled(isLoading)
                    }
                }

                if let result {
                    VinResultSection(result: result)
                }

                Section {
                    Text("Vehicle data is decoded by the US National Highway Traffic Safety Administration (NHTSA) vPIC service.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("VIN decoder")
        }
    }

    private func startDecode() {
        let normalized = VinInput.shared.normalize(value: vin)
        vin = normalized
        guard VinInput.shared.isValid(vin: normalized) else {
            validationError = true
            loadFailed = false
            result = nil
            return
        }

        requestGeneration += 1
        let generation = requestGeneration
        validationError = false
        loadFailed = false
        result = nil
        isLoading = true

        Task {
            await decode(normalized, generation: generation)
        }
    }

    @MainActor
    private func decode(_ vin: String, generation: Int) async {
        defer {
            if requestGeneration == generation {
                isLoading = false
            }
        }

        guard let url = URL(
            string: "https://vpic.nhtsa.dot.gov/api/vehicles/DecodeVinValues/\(vin)?format=json"
        ) else {
            loadFailed = true
            return
        }

        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            guard requestGeneration == generation,
                  let httpResponse = response as? HTTPURLResponse,
                  (200...299).contains(httpResponse.statusCode),
                  let payload = String(data: data, encoding: .utf8),
                  let decoded = VpicDecoder.shared.decodeJsonOrNull(
                    payload: payload,
                    fallbackVin: vin
                  ) else {
                if requestGeneration == generation {
                    loadFailed = true
                }
                return
            }
            result = decoded
        } catch {
            if requestGeneration == generation {
                loadFailed = true
            }
        }
    }
}

private struct VinResultSection: View {
    let result: VinDecodeResult

    private var summary: String {
        [result.modelYear, result.make, result.model]
            .compactMap { $0 }
            .joined(separator: " ")
    }

    private var plant: String? {
        let value = [result.plantCity, result.plantCountry]
            .compactMap { $0 }
            .joined(separator: ", ")
        return value.isEmpty ? nil : value
    }

    var body: some View {
        Section {
            Text(summary.isEmpty ? result.vin : summary)
                .font(.headline)

            if let warning = result.decoderWarning {
                Text("Decoder warning: \(warning)")
                    .font(.footnote)
                    .foregroundStyle(.red)
            }

            if !summary.isEmpty { VinRow("VIN", result.vin) }
            VinRow("Make", result.make)
            VinRow("Model", result.model)
            VinRow("Year", result.modelYear)
            VinRow("Trim", result.trim)
            VinRow("Series", result.series)
            VinRow("Vehicle type", result.vehicleType)
            VinRow("Body", result.bodyClass)
            VinRow("Fuel", result.fuelType)
            VinRow("Electrification", result.electrificationLevel)
            VinRow("Displacement", result.displacementLiters.map { "\($0) L" })
            VinRow("Cylinders", result.engineCylinders)
            VinRow("Power", result.engineHp.map { "\($0) hp" })
            VinRow("Drive", result.driveType)
            VinRow("Transmission", result.transmissionStyle)
            VinRow("Plant", plant)
        } header: {
            Text("Decoded vehicle")
        }
    }
}

private struct VinRow: View {
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
        }
    }
}
