import AutkaShared
import Foundation

enum SourceCatalog {
    static func fetch() async -> [SourceHealth]? {
        let url = AppConfiguration.backendBaseURL.appendingPathComponent("sources")
        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            guard let httpResponse = response as? HTTPURLResponse,
                  (200...299).contains(httpResponse.statusCode),
                  let payload = String(data: data, encoding: .utf8) else {
                return nil
            }
            return SourceHealthDecoder.shared.decodeJsonOrNull(payload: payload)
        } catch {
            return nil
        }
    }
}
