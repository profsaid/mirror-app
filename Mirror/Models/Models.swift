import Foundation

/// The three photos that make up a head scan. Named after the side of the head
/// the camera sees, so `.right` means the right ear faces the camera.
enum HeadAngle: String, CaseIterable, Codable, Identifiable, Hashable {
    case front, right, left

    var id: String { rawValue }

    /// Short label for chips and thumbnails.
    var label: String {
        switch self {
        case .front: "Front"
        case .right: "Right"
        case .left: "Left"
        }
    }

    /// Label for the segmented control on the result screen.
    var title: String {
        switch self {
        case .front: "Front"
        case .right: "Right side"
        case .left: "Left side"
        }
    }

    var instruction: String {
        switch self {
        case .front: "Look straight ahead"
        case .right: "Turn your head left"
        case .left: "Turn your head right"
        }
    }

    var detail: String {
        switch self {
        case .front: "Chin level, hair dry and the way you usually wear it."
        case .right: "Keep turning until your right ear faces the camera. Shoulders stay still."
        case .left: "Same again the other way, until your left ear faces the camera."
        }
    }

    var next: HeadAngle? {
        switch self {
        case .front: .right
        case .right: .left
        case .left: nil
        }
    }
}

enum HairLength: String, CaseIterable, Codable, Identifiable {
    case short = "Short"
    case medium = "Medium"
    case long = "Long"
    case curly = "Curly"

    var id: String { rawValue }
}

struct BarberNote: Hashable, Codable {
    let label: String
    let value: String
}

struct Hairstyle: Identifiable, Hashable, Codable {
    let id: String
    let name: String
    let tag: String
    let length: HairLength
    let notes: [BarberNote]
    /// Plain-language description sent to the image model.
    let description: String
    /// Illustration paths in a 200×240 box (same art as the Figma/canvas mockups).
    let frontPath: String
    let sidePath: String

    /// Text that goes with the photos when the look is shared with a barber.
    var barberText: String {
        let lines = notes.map { "\($0.label): \($0.value)" }.joined(separator: "\n")
        return "\(name)\n\(lines)"
    }
}

/// A try-on the user chose to keep. Images live on disk; this is the index entry.
struct SavedLook: Identifiable, Codable, Hashable {
    let id: UUID
    let styleID: String
    let createdAt: Date

    func fileName(for angle: HeadAngle) -> String {
        "look-\(id.uuidString)-\(angle.rawValue)"
    }
}
