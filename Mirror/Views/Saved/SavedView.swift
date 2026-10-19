import SwiftUI

struct SavedView: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        NavigationStack {
            Group {
                if model.saved.isEmpty {
                    ContentUnavailableView {
                        Label("No saved looks", systemImage: "heart")
                    } description: {
                        Text("Tap the heart on a try-on to keep it here and show it to your barber.")
                    } actions: {
                        Button("Browse styles") { model.tab = .styles }
                            .buttonStyle(.borderedProminent)
                            .foregroundStyle(.black)
                    }
                } else {
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            Text("Show any of these to your barber.")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            ForEach(model.saved) { look in
                                NavigationLink(value: look) {
                                    SavedCard(look: look, images: model.images(for: look))
                                }
                                .buttonStyle(PressableStyle())
                                .contextMenu {
                                    Button("Delete", systemImage: "trash", role: .destructive) {
                                        withAnimation { model.delete(look) }
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 16)
                    }
                }
            }
            .navigationTitle("Saved")
            .navigationDestination(for: SavedLook.self) { look in
                SavedLookDestination(look: look)
            }
        }
    }
}

private struct SavedCard: View {
    let look: SavedLook
    let images: [HeadAngle: UIImage]
    private var style: Hairstyle? { HairstyleCatalog.style(id: look.styleID) }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(style?.name ?? "Look").font(.headline).foregroundStyle(.white)
                    Text(look.createdAt, format: .dateTime.day().month(.abbreviated))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
            HStack(spacing: 8) {
                ForEach(HeadAngle.allCases) { angle in
                    AngleThumbnail(angle: angle, image: images[angle], style: style, height: 96, cornerRadius: 10)
                }
            }
        }
        .padding(14)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}
