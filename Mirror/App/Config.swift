import Foundation

/// App-wide settings you are expected to edit.
enum Config {
    /// The URL of your own try-on backend (see README → "Backend").
    /// While this is `nil` the app uses `MockTryOnService`, so every screen works
    /// end to end without an AI key.
    static let tryOnEndpoint: URL? = nil

    /// Vision reports head yaw with a sign that depends on camera mirroring.
    /// If the app asks you to turn the wrong way on a real iPhone, change this to -1.
    static let yawSign: Double = 1

    /// How far the head must turn (in degrees) before a side photo counts.
    static let sideYawThreshold: Double = 35

    /// How close to straight-on the front photo must be (in degrees).
    static let frontYawTolerance: Double = 12

    /// How long the head must stay lined up before the photo is taken automatically.
    static let holdStillSeconds: Double = 0.8
}
