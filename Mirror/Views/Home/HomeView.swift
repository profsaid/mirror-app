import SwiftUI

struct HomeView: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("See a haircut from the front and both sides before the barber picks up the clippers.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)

                    scanCard
                    recentSection
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
            .navigationTitle("Mirror")
            .navigationDestination(for: SavedLook.self) { look in
                SavedLookDestination(look: look)
            }
        }
    }

    private var scanCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Your head scan").font(.headline)
                Spacer()
                if model.hasCompleteScan {
                    Label("3 of 3 angles", systemImage: "checkmark")
                        .font(.footnote)
                        .foregroundStyle(.green)
                } else {
                    Text("Not scanned yet")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }

            HStack(spacing: 10) {
                ForEach(HeadAngle.allCases) { angle in
                    VStack(spacing: 6) {
                        AngleThumbnail(angle: angle, image: model.scan[angle])
                        Text(angle.label).font(.footnote).foregroundStyle(.secondary)
                    }
                }
            }

            PrimaryButton(title: model.hasCompleteScan ? "New scan" : "Start scan", systemImage: "camera") {
                model.showCapture = true
            }
        }
        .padding(16)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    @ViewBuilder
    private var recentSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                Text("Recent try-ons").font(.title2.weight(.bold))
                Spacer()
                if !model.saved.isEmpty {
                    Button("See all") { model.tab = .saved }
                }
            }

            if model.saved.isEmpty {
                Text("Looks you save show up here.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, minHeight: 64)
                    .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(model.saved.prefix(3).enumerated()), id: \.element.id) { index, look in
                        NavigationLink(value: look) {
                            RecentRow(look: look)
                        }
                        .buttonStyle(.plain)
                        if index < min(model.saved.count, 3) - 1 {
                            Divider().padding(.leading, 76)
                        }
                    }
                }
                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
        }
    }
}

private struct RecentRow: View {
    let look: SavedLook
    private var style: Hairstyle? { HairstyleCatalog.style(id: look.styleID) }

    var body: some View {
        HStack(spacing: 12) {
            HeadIllustration(angle: .front, style: style)
                .frame(width: 48, height: 48)
                .scaleEffect(1.35)
                .offset(y: 6)
                .frame(width: 48, height: 48)
                .background(Color(.tertiarySystemBackground))
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(style?.name ?? "Look").font(.body)
                Text(look.createdAt, format: .relative(presentation: .named))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 16)
        .frame(minHeight: 64)
        .contentShape(Rectangle())
    }
}

/// Loads a saved look's images from disk and shows it on the result screen.
struct SavedLookDestination: View {
    @Environment(AppModel.self) private var model
    let look: SavedLook

    var body: some View {
        if let style = HairstyleCatalog.style(id: look.styleID) {
            ResultView(look: look, style: style, images: model.images(for: look))
        } else {
            ContentUnavailableView("Style not found", systemImage: "scissors")
        }
    }
}
