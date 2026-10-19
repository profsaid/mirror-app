import SwiftUI
import Observation

enum AppTab: Hashable {
    case home, styles, saved
}

@Observable
@MainActor
final class AppModel {
    var tab: AppTab = .home
    var showCapture = false

    /// The current head scan, one photo per angle.
    private(set) var scan: [HeadAngle: UIImage] = [:]
    /// Newest first.
    private(set) var saved: [SavedLook] = []

    let tryOn: TryOnService

    init(tryOn: TryOnService? = nil) {
        self.tryOn = tryOn ?? TryOnServiceFactory.make()
        loadFromDisk()
    }

    var hasCompleteScan: Bool {
        HeadAngle.allCases.allSatisfy { scan[$0] != nil }
    }

    // MARK: Scan

    func replaceScan(with images: [HeadAngle: UIImage]) {
        scan = images
        for (angle, image) in images {
            ImageStore.save(image, name: Self.scanFileName(angle))
        }
    }

    // MARK: Saved looks

    @discardableResult
    func save(results: [HeadAngle: UIImage], style: Hairstyle) -> SavedLook {
        let look = SavedLook(id: UUID(), styleID: style.id, createdAt: .now)
        for (angle, image) in results {
            ImageStore.save(image, name: look.fileName(for: angle))
        }
        saved.insert(look, at: 0)
        persistIndex()
        return look
    }

    func delete(_ look: SavedLook) {
        for angle in HeadAngle.allCases {
            ImageStore.delete(name: look.fileName(for: angle))
        }
        saved.removeAll { $0.id == look.id }
        persistIndex()
    }

    func images(for look: SavedLook) -> [HeadAngle: UIImage] {
        var result: [HeadAngle: UIImage] = [:]
        for angle in HeadAngle.allCases {
            if let image = ImageStore.load(name: look.fileName(for: angle)) {
                result[angle] = image
            }
        }
        return result
    }

    // MARK: Persistence

    private static func scanFileName(_ angle: HeadAngle) -> String { "scan-\(angle.rawValue)" }
    private static let indexURL = ImageStore.directory.appendingPathComponent("saved.json")

    private func loadFromDisk() {
        for angle in HeadAngle.allCases {
            if let image = ImageStore.load(name: Self.scanFileName(angle)) {
                scan[angle] = image
            }
        }
        if let data = try? Data(contentsOf: Self.indexURL),
           let looks = try? JSONDecoder().decode([SavedLook].self, from: data) {
            saved = looks
        }
    }

    private func persistIndex() {
        do {
            let data = try JSONEncoder().encode(saved)
            try data.write(to: Self.indexURL, options: .atomic)
        } catch {
            print("Could not save index: \(error)")
        }
    }
}

/// JPEGs in the app's Documents folder. Everything stays on the phone.
enum ImageStore {
    static let directory: URL = {
        let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("Mirror", isDirectory: true)
        try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        return url
    }()

    private static func url(_ name: String) -> URL {
        directory.appendingPathComponent(name).appendingPathExtension("jpg")
    }

    static func save(_ image: UIImage, name: String) {
        guard let data = image.jpegData(compressionQuality: 0.88) else { return }
        try? data.write(to: url(name), options: .atomic)
    }

    static func load(name: String) -> UIImage? {
        UIImage(contentsOfFile: url(name).path)
    }

    static func delete(name: String) {
        try? FileManager.default.removeItem(at: url(name))
    }
}

extension UIImage {
    /// Shrinks the image so its longest side is at most `maxSide` points, keeping orientation upright.
    func resized(maxSide: CGFloat) -> UIImage {
        let longest = max(size.width, size.height)
        guard longest > maxSide else { return normalizedOrientation() }
        let scale = maxSide / longest
        let target = CGSize(width: (size.width * scale).rounded(), height: (size.height * scale).rounded())
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        return UIGraphicsImageRenderer(size: target, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: target))
        }
    }

    func normalizedOrientation() -> UIImage {
        guard imageOrientation != .up else { return self }
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = scale
        return UIGraphicsImageRenderer(size: size, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: size))
        }
    }
}
