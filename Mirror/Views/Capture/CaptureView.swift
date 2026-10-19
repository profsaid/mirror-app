import PhotosUI
import SwiftUI

/// Guided three-angle scan: front, right side, left side.
struct CaptureView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    @StateObject private var camera = CameraController()

    @State private var step: HeadAngle = .front
    @State private var captured: [HeadAngle: UIImage] = [:]
    @State private var isShooting = false
    @State private var pickerItem: PhotosPickerItem?
    @State private var errorMessage: String?

    private var isDone: Bool { HeadAngle.allCases.allSatisfy { captured[$0] != nil } }
    private var cameraUsable: Bool { camera.status != .unavailable && camera.status != .denied }

    var body: some View {
        VStack(spacing: 16) {
            topBar
            viewport
            angleTabs
            if isDone { finishControls } else { shutterControls }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 8)
        .background(Color.black.ignoresSafeArea())
        .task {
            camera.target = step
            camera.onReadyToCapture = { Task { await shoot() } }
            await camera.start()
        }
        .onDisappear { camera.stop() }
        .onChange(of: step) { _, newValue in camera.target = newValue }
        .onChange(of: pickerItem) { _, item in
            guard let item else { return }
            Task { await importPhoto(item) }
        }
        .alert("Couldn't take the photo", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK") { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
        .sensoryFeedback(.success, trigger: captured.count)
    }

    // MARK: Top bar

    private var topBar: some View {
        HStack {
            CircleIconButton(systemName: "xmark", label: "Close") { dismiss() }
            Spacer()
            Text(isDone ? "All set" : "Step \(stepNumber) of 3")
                .font(.subheadline.weight(.semibold))
                .padding(.horizontal, 14)
                .frame(height: 32)
                .background(Color(.secondarySystemBackground), in: Capsule())
            Spacer()
            // Keeps the title centred; becomes a Photos picker when there's no camera.
            if cameraUsable {
                Color.clear.frame(width: 44, height: 44)
            } else {
                PhotosPicker(selection: $pickerItem, matching: .images) {
                    Image(systemName: "photo.on.rectangle")
                        .font(.system(size: 17, weight: .semibold))
                        .frame(width: 44, height: 44)
                        .background(Color(.secondarySystemBackground), in: Circle())
                }
                .accessibilityLabel("Choose a photo")
            }
        }
        .foregroundStyle(.white)
    }

    private var stepNumber: Int {
        (HeadAngle.allCases.firstIndex(of: step) ?? 0) + 1
    }

    // MARK: Viewport

    private var viewport: some View {
        ZStack {
            if isDone {
                doneGrid
            } else if let photo = captured[step] {
                Image(uiImage: photo).resizable().scaledToFill()
            } else if cameraUsable {
                CameraPreview(session: camera.session)
                guideOverlay
            } else {
                noCameraPlaceholder
            }

            VStack(spacing: 6) {
                Text(isDone ? "All 3 angles captured" : step.instruction)
                    .font(.title2.weight(.bold))
                Text(isDone ? "Your scan is ready. Pick a haircut to try on." : step.detail)
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.75))
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 24)
            .padding(.top, 20)
            .frame(maxHeight: .infinity, alignment: .top)
            .shadow(color: .black.opacity(0.6), radius: 8)

            statusPill
                .padding(.bottom, 16)
                .frame(maxHeight: .infinity, alignment: .bottom)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 480)
        .background(Color(white: 0.055))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .strokeBorder(Color(.systemGray5), lineWidth: 0.5)
        )
    }

    private var guideOverlay: some View {
        Ellipse()
            .strokeBorder(
                camera.status.isAligned ? Color.green : Color.accentColor,
                style: StrokeStyle(lineWidth: 3, dash: camera.status.isAligned ? [] : [10, 8])
            )
            .frame(width: 230, height: 300)
            .overlay {
                Ellipse()
                    .trim(from: 0, to: camera.holdProgress)
                    .stroke(Color.green, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                    .rotationEffect(.degrees(-90))
            }
            .offset(y: 30)
            .animation(.easeOut(duration: 0.15), value: camera.holdProgress)
            .accessibilityHidden(true)
    }

    private var noCameraPlaceholder: some View {
        VStack(spacing: 16) {
            HeadIllustration(angle: step, style: nil, hairColor: Color(.systemGray3))
                .frame(height: 260)
                .opacity(0.6)
            PhotosPicker(selection: $pickerItem, matching: .images) {
                Label("Choose \(step.label.lowercased()) photo", systemImage: "photo")
                    .font(.headline)
                    .padding(.horizontal, 18)
                    .frame(height: 44)
                    .background(Color(.secondarySystemBackground), in: Capsule())
            }
            if camera.status == .denied {
                Button("Turn on camera in Settings") {
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        UIApplication.shared.open(url)
                    }
                }
                .font(.subheadline)
            }
        }
        .padding(.top, 70)
    }

    private var doneGrid: some View {
        HStack(spacing: 8) {
            ForEach(HeadAngle.allCases) { angle in
                VStack(spacing: 6) {
                    if let image = captured[angle] {
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFill()
                            .frame(height: 180)
                            .frame(maxWidth: .infinity)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    Text(angle.label).font(.footnote).foregroundStyle(.secondary)
                }
            }
        }
        .padding(.horizontal, 12)
        .padding(.top, 60)
    }

    private var statusPill: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(isDone || camera.status.isAligned ? Color.green : Color.accentColor)
                .frame(width: 8, height: 8)
            Text(statusText).font(.footnote)
        }
        .padding(.horizontal, 12)
        .frame(height: 30)
        .background(Color(.secondarySystemBackground), in: Capsule())
    }

    private var statusText: String {
        if isDone { return "Scan saved on this iPhone" }
        if captured[step] != nil { return "Photo taken" }
        return camera.status.message
    }

    // MARK: Angle tabs

    private var angleTabs: some View {
        HStack(spacing: 8) {
            ForEach(HeadAngle.allCases) { angle in
                let active = !isDone && angle == step
                Button {
                    step = angle
                } label: {
                    HStack(spacing: 6) {
                        if captured[angle] != nil {
                            Image(systemName: "checkmark").foregroundStyle(.green)
                        }
                        Text(angle.label)
                    }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(active || captured[angle] != nil ? Color.white : Color.secondary)
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .strokeBorder(active ? Color.accentColor : .clear, lineWidth: 1.5)
                    )
                }
                .accessibilityAddTraits(active ? .isSelected : [])
            }
        }
    }

    // MARK: Bottom controls

    private var shutterControls: some View {
        VStack(spacing: 14) {
            Text(cameraUsable
                 ? "Takes the photo automatically when you're lined up, or tap the shutter."
                 : "No camera here. Pick a photo for each angle instead.")
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            HStack {
                lastThumbnail.frame(width: 64, alignment: .leading)
                Spacer()
                Button {
                    Task { await shoot() }
                } label: {
                    Circle()
                        .fill(.white)
                        .padding(5)
                        .overlay(Circle().strokeBorder(.white, lineWidth: 4))
                        .frame(width: 76, height: 76)
                }
                .disabled(!cameraUsable || isShooting || captured[step] != nil)
                .opacity(cameraUsable ? 1 : 0.3)
                .accessibilityLabel("Take photo")
                Spacer()
                Button("Retake") { retake() }
                    .font(.body)
                    .frame(width: 64, height: 44, alignment: .trailing)
                    .disabled(captured[step] == nil)
            }
        }
    }

    @ViewBuilder
    private var lastThumbnail: some View {
        let previous = HeadAngle.allCases.last { captured[$0] != nil && $0 != step }
        if let previous, let image = captured[previous] {
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
                .frame(width: 48, height: 58)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).strokeBorder(.white, lineWidth: 1.5))
        } else {
            Color.clear.frame(width: 48, height: 58)
        }
    }

    private var finishControls: some View {
        VStack(spacing: 4) {
            PrimaryButton(title: "Choose a style", systemImage: "scissors") {
                model.replaceScan(with: captured)
                model.tab = .styles
                dismiss()
            }
            Button("Retake all") {
                captured.removeAll()
                step = .front
                camera.isPaused = false
            }
            .frame(height: 44)
        }
    }

    // MARK: Actions

    @MainActor
    private func shoot() async {
        guard cameraUsable, !isShooting, captured[step] == nil else { return }
        isShooting = true
        defer { isShooting = false }
        do {
            let photo = try await camera.capturePhoto()
            store(photo)
        } catch {
            errorMessage = error.localizedDescription
            camera.target = step
        }
    }

    @MainActor
    private func importPhoto(_ item: PhotosPickerItem) async {
        defer { pickerItem = nil }
        guard let data = try? await item.loadTransferable(type: Data.self),
              let image = UIImage(data: data) else {
            errorMessage = "That photo couldn't be opened."
            return
        }
        store(image.normalizedOrientation())
    }

    private func store(_ photo: UIImage) {
        captured[step] = photo.resized(maxSide: 1600)
        if let next = HeadAngle.allCases.first(where: { captured[$0] == nil }) {
            // Short pause so the user sees the photo before the next step.
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { step = next }
        } else {
            camera.isPaused = true
        }
    }

    private func retake() {
        captured[step] = nil
        camera.isPaused = false
        camera.target = step
    }
}
