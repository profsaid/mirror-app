import AVFoundation
import Combine
import UIKit
import Vision

/// Front camera + live head-pose tracking. Publishes whether the head is lined up
/// for the current target angle and asks the view to shoot once it has held still.
final class CameraController: NSObject, ObservableObject {
    enum Status: Equatable {
        case starting
        case unavailable          // simulator, or no front camera
        case denied               // user refused camera access
        case noFace
        case adjust(String)
        case holdStill
        case capturing

        var message: String {
            switch self {
            case .starting: "Starting camera…"
            case .unavailable: "Camera isn't available here"
            case .denied: "Camera access is off"
            case .noFace: "Line your face up with the outline"
            case .adjust(let hint): hint
            case .holdStill: "Hold still…"
            case .capturing: "Got it"
            }
        }

        var isAligned: Bool { self == .holdStill || self == .capturing }
    }

    @Published private(set) var status: Status = .starting
    @Published private(set) var holdProgress: Double = 0

    /// Called on the main thread when the head has been lined up long enough.
    var onReadyToCapture: (() -> Void)?

    let session = AVCaptureSession()

    private let sessionQueue = DispatchQueue(label: "mirror.camera.session")
    private let videoQueue = DispatchQueue(label: "mirror.camera.video")
    private let photoOutput = AVCapturePhotoOutput()
    private let videoOutput = AVCaptureVideoDataOutput()
    private var isConfigured = false

    // Shared between the main thread and the video queue.
    private let lock = NSLock()
    private var _target: HeadAngle = .front
    private var _paused = false
    private var alignedSince: Date?
    private var didRequestCapture = false
    private var lastAnalysis = Date.distantPast

    private var photoContinuation: CheckedContinuation<UIImage, Error>?

    var target: HeadAngle {
        get { lock.withLock { _target } }
        set {
            lock.withLock {
                _target = newValue
                alignedSince = nil
                didRequestCapture = false
            }
        }
    }

    /// Stops head tracking (e.g. once all photos are taken) without tearing down the session.
    var isPaused: Bool {
        get { lock.withLock { _paused } }
        set { lock.withLock { _paused = newValue; alignedSince = nil; didRequestCapture = false } }
    }

    // MARK: Lifecycle

    func start() async {
        let granted: Bool
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: granted = true
        case .notDetermined: granted = await AVCaptureDevice.requestAccess(for: .video)
        default: granted = false
        }
        guard granted else {
            await MainActor.run { self.status = .denied }
            return
        }
        sessionQueue.async { [weak self] in
            guard let self else { return }
            if !self.isConfigured {
                guard self.configure() else {
                    DispatchQueue.main.async { self.status = .unavailable }
                    return
                }
            }
            if !self.session.isRunning { self.session.startRunning() }
            DispatchQueue.main.async { self.status = .noFace }
        }
    }

    func stop() {
        sessionQueue.async { [weak self] in
            guard let self, self.session.isRunning else { return }
            self.session.stopRunning()
        }
    }

    private func configure() -> Bool {
        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .front),
              let input = try? AVCaptureDeviceInput(device: device) else {
            return false
        }
        session.beginConfiguration()
        defer { session.commitConfiguration() }
        session.sessionPreset = .photo

        guard session.canAddInput(input), session.canAddOutput(photoOutput) else { return false }
        session.addInput(input)
        session.addOutput(photoOutput)

        videoOutput.alwaysDiscardsLateVideoFrames = true
        videoOutput.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        videoOutput.setSampleBufferDelegate(self, queue: videoQueue)
        if session.canAddOutput(videoOutput) { session.addOutput(videoOutput) }

        // Upright portrait photos.
        if let connection = photoOutput.connection(with: .video), connection.isVideoRotationAngleSupported(90) {
            connection.videoRotationAngle = 90
        }
        isConfigured = true
        return true
    }

    // MARK: Photo

    func capturePhoto() async throws -> UIImage {
        await MainActor.run { self.status = .capturing }
        return try await withCheckedThrowingContinuation { continuation in
            sessionQueue.async { [weak self] in
                guard let self else { return }
                self.photoContinuation = continuation
                let settings = AVCapturePhotoSettings()
                settings.photoQualityPrioritization = .balanced
                self.photoOutput.capturePhoto(with: settings, delegate: self)
            }
        }
    }

    // MARK: Head pose

    private func evaluate(yawDegrees yaw: Double?, target: HeadAngle) -> Status {
        guard let yaw else { return .noFace }
        let threshold = Config.sideYawThreshold
        switch target {
        case .front:
            if abs(yaw) <= Config.frontYawTolerance { return .holdStill }
            return .adjust("Face the camera straight on")
        case .right:
            if yaw >= threshold { return .holdStill }
            if yaw < -10 { return .adjust("Other way: turn left") }
            return .adjust("Keep turning left")
        case .left:
            if yaw <= -threshold { return .holdStill }
            if yaw > 10 { return .adjust("Other way: turn right") }
            return .adjust("Keep turning right")
        }
    }
}

// MARK: - Video frames

extension CameraController: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        let (target, paused, shouldAnalyze) = lock.withLock { () -> (HeadAngle, Bool, Bool) in
            let now = Date()
            let due = now.timeIntervalSince(lastAnalysis) > 0.1   // ~10 analyses per second
            if due { lastAnalysis = now }
            return (_target, _paused, due)
        }
        guard !paused, shouldAnalyze, let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        let request = VNDetectFaceRectanglesRequest()
        request.revision = VNDetectFaceRectanglesRequestRevision3
        // Front camera held in portrait.
        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .leftMirrored, options: [:])
        try? handler.perform([request])

        let face = request.results?.max { $0.boundingBox.width < $1.boundingBox.width }
        let yaw = face?.yaw.map { $0.doubleValue * 180 / .pi * Config.yawSign }
        let status = evaluate(yawDegrees: yaw, target: target)

        // Hold-still timer.
        let (progress, fire) = lock.withLock { () -> (Double, Bool) in
            guard status.isAligned else {
                alignedSince = nil
                return (0, false)
            }
            let start = alignedSince ?? Date()
            alignedSince = start
            let held = Date().timeIntervalSince(start)
            let progress = min(held / Config.holdStillSeconds, 1)
            if progress >= 1 && !didRequestCapture {
                didRequestCapture = true
                return (1, true)
            }
            return (progress, false)
        }

        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            if self.status != .capturing { self.status = status }
            self.holdProgress = progress
            if fire { self.onReadyToCapture?() }
        }
    }
}

// MARK: - Photo delegate

extension CameraController: AVCapturePhotoCaptureDelegate {
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        let continuation = photoContinuation
        photoContinuation = nil
        DispatchQueue.main.async { [weak self] in
            self?.status = .noFace
            self?.holdProgress = 0
        }
        if let error {
            continuation?.resume(throwing: error)
        } else if let data = photo.fileDataRepresentation(), let image = UIImage(data: data) {
            continuation?.resume(returning: image.normalizedOrientation())
        } else {
            continuation?.resume(throwing: CocoaError(.fileReadCorruptFile))
        }
    }
}
