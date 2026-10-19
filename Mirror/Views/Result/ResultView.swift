import SwiftUI

/// Shows a haircut on all three angles, with before/after and barber notes.
struct ResultView: View {
    @Environment(AppModel.self) private var model

    enum Phase: Equatable {
        case loading
        case ready
        case failed(String)
    }

    let style: Hairstyle
    private let isFromSaved: Bool

    @State private var results: [HeadAngle: UIImage]
    @State private var phase: Phase
    @State private var savedID: UUID?
    @State private var angle: HeadAngle = .front
    @State private var showBefore = false

    /// A fresh try-on: runs the try-on service when the screen appears.
    init(style: Hairstyle) {
        self.style = style
        self.isFromSaved = false
        _results = State(initialValue: [:])
        _phase = State(initialValue: .loading)
        _savedID = State(initialValue: nil)
    }

    /// A look the user already saved.
    init(look: SavedLook, style: Hairstyle, images: [HeadAngle: UIImage]) {
        self.style = style
        self.isFromSaved = true
        _results = State(initialValue: images)
        _phase = State(initialValue: .ready)
        _savedID = State(initialValue: look.id)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                imageArea
                Picker("Angle", selection: $angle) {
                    ForEach(HeadAngle.allCases) { Text($0.title).tag($0) }
                }
                .pickerStyle(.segmented)
                barberCard
                actions
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
        }
        .navigationTitle(style.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if !isFromSaved {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        Task { await generate() }
                    } label: {
                        Image(systemName: "arrow.counterclockwise")
                    }
                    .disabled(phase == .loading)
                    .accessibilityLabel("Try again")
                }
            }
        }
        .task {
            if phase == .loading { await generate() }
        }
    }

    // MARK: Image

    private var imageArea: some View {
        ZStack {
            Color(.secondarySystemBackground)

            switch phase {
            case .loading:
                VStack(spacing: 14) {
                    ProgressView().controlSize(.large)
                    Text("Trying on \(style.name)…")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text("Usually takes a few seconds.")
                        .font(.footnote)
                        .foregroundStyle(.tertiary)
                }
            case .failed(let message):
                VStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.largeTitle)
                        .foregroundStyle(.secondary)
                    Text(message)
                        .font(.subheadline)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                    Button("Try again") { Task { await generate() } }
                        .buttonStyle(.bordered)
                }
            case .ready:
                displayedImage
            }

            if phase == .ready {
                VStack {
                    HStack {
                        Label("AI preview", systemImage: "sparkles")
                            .font(.footnote)
                            .padding(.horizontal, 10)
                            .frame(height: 28)
                            .background(.ultraThinMaterial, in: Capsule())
                        Spacer()
                    }
                    Spacer()
                    beforeAfterToggle
                }
                .padding(14)
            }
        }
        .frame(height: 420)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .animation(.easeInOut(duration: 0.2), value: showBefore)
        .animation(.easeInOut(duration: 0.2), value: angle)
    }

    @ViewBuilder
    private var displayedImage: some View {
        let image = showBefore ? model.scan[angle] : results[angle]
        if let image {
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .clipped()
                .accessibilityLabel(showBefore ? "Current hair, \(angle.title)" : "\(style.name), \(angle.title)")
        } else {
            HeadIllustration(angle: angle, style: showBefore ? nil : style,
                             hairColor: showBefore ? Color(.systemGray2) : .accentColor)
                .padding(40)
        }
    }

    private var beforeAfterToggle: some View {
        HStack(spacing: 2) {
            toggleSegment("Before", isOn: showBefore) { showBefore = true }
            toggleSegment("After", isOn: !showBefore) { showBefore = false }
        }
        .padding(3)
        .background(Color.black.opacity(0.8), in: Capsule())
    }

    private func toggleSegment(_ title: String, isOn: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(isOn ? Color.black : Color.white)
                .padding(.horizontal, 16)
                .frame(minHeight: 30)
                .background(isOn ? Color.white : Color.clear, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }

    // MARK: Barber notes

    private var barberCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("FOR YOUR BARBER")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.secondary)
            Grid(alignment: .leading, horizontalSpacing: 12, verticalSpacing: 6) {
                ForEach(style.notes, id: \.self) { note in
                    GridRow {
                        Text(note.label).foregroundStyle(.secondary)
                        Text(note.value)
                    }
                    .font(.subheadline)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    // MARK: Actions

    private var shareImages: [Image] {
        HeadAngle.allCases.compactMap { results[$0] }.map { Image(uiImage: $0) }
    }

    private var actions: some View {
        HStack(spacing: 10) {
            Button(action: toggleSaved) {
                Image(systemName: savedID == nil ? "heart" : "heart.fill")
                    .font(.title3)
                    .foregroundStyle(savedID == nil ? Color.white : Color.red)
                    .frame(width: 50, height: 50)
                    .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .buttonStyle(PressableStyle())
            .disabled(phase != .ready)
            .accessibilityLabel(savedID == nil ? "Save look" : "Remove from saved")
            .sensoryFeedback(.impact, trigger: savedID)

            ShareLink(
                items: shareImages,
                subject: Text(style.name),
                message: Text(style.barberText),
                preview: { image in SharePreview(style.name, image: image) }
            ) {
                Label("Send to my barber", systemImage: "square.and.arrow.up")
                    .font(.headline)
                    .foregroundStyle(.black)
                    .frame(maxWidth: .infinity, minHeight: 50)
                    .background(Color.accentColor, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .disabled(phase != .ready || shareImages.isEmpty)
        }
    }

    private func toggleSaved() {
        if let id = savedID, let look = model.saved.first(where: { $0.id == id }) {
            model.delete(look)
            savedID = nil
        } else {
            savedID = model.save(results: results, style: style).id
        }
    }

    @MainActor
    private func generate() async {
        phase = .loading
        savedID = nil
        do {
            results = try await model.tryOn.apply(style: style, to: model.scan)
            phase = .ready
        } catch is CancellationError {
            // Screen closed while waiting; nothing to show.
        } catch {
            phase = .failed(error.localizedDescription)
        }
    }
}
