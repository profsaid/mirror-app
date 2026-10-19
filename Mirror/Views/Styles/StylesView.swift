import SwiftUI

struct StylesView: View {
    @Environment(AppModel.self) private var model

    @State private var filter: HairLength?
    @State private var search = ""
    @State private var selectedID = HairstyleCatalog.all[0].id
    @State private var tryOnStyle: Hairstyle?

    private let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]

    private var visibleStyles: [Hairstyle] {
        HairstyleCatalog.all.filter { style in
            (filter == nil || style.length == filter) &&
            (search.isEmpty || style.name.localizedCaseInsensitiveContains(search) || style.tag.localizedCaseInsensitiveContains(search))
        }
    }

    private var selectedStyle: Hairstyle {
        HairstyleCatalog.style(id: selectedID) ?? HairstyleCatalog.all[0]
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    filterChips
                    if visibleStyles.isEmpty {
                        ContentUnavailableView.search(text: search)
                    } else {
                        LazyVGrid(columns: columns, spacing: 12) {
                            ForEach(visibleStyles) { style in
                                StyleCard(style: style, isSelected: style.id == selectedID) {
                                    selectedID = style.id
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                }
                .padding(.bottom, 16)
            }
            .navigationTitle("Styles")
            .searchable(text: $search, prompt: "Search haircuts")
            .safeAreaInset(edge: .bottom) { tryOnBar }
            .navigationDestination(item: $tryOnStyle) { style in
                ResultView(style: style)
            }
        }
    }

    private var filterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                chip(title: "All", value: nil)
                ForEach(HairLength.allCases) { length in
                    chip(title: length.rawValue, value: length)
                }
            }
            .padding(.horizontal, 16)
        }
    }

    private func chip(title: String, value: HairLength?) -> some View {
        let active = filter == value
        return Button {
            withAnimation(.snappy) { filter = value }
        } label: {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(active ? Color.black : Color.white)
                .padding(.horizontal, 16)
                .frame(minHeight: 34)
                .background(active ? Color.accentColor : Color(.secondarySystemBackground), in: Capsule())
        }
        .buttonStyle(PressableStyle())
        .accessibilityAddTraits(active ? .isSelected : [])
    }

    private var tryOnBar: some View {
        Group {
            if model.hasCompleteScan {
                PrimaryButton(title: "Try on \(selectedStyle.name)", systemImage: "sparkles") {
                    tryOnStyle = selectedStyle
                }
            } else {
                PrimaryButton(title: "Scan your head first", systemImage: "camera") {
                    model.showCapture = true
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(.black)
    }
}

private struct StyleCard: View {
    let style: Hairstyle
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 8) {
                HeadIllustration(angle: .front, style: style, hairColor: isSelected ? .accentColor : Color(.systemGray4))
                    .padding(.top, 10)
                    .frame(maxWidth: .infinity)
                    .frame(height: 140)
                    .background(Color(.tertiarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text(style.name).font(.subheadline.weight(.semibold)).foregroundStyle(.white)
                    Text(style.tag).font(.footnote).foregroundStyle(.secondary)
                }
                .padding(.horizontal, 4)
                .padding(.bottom, 4)
            }
            .padding(8)
            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .strokeBorder(isSelected ? Color.accentColor : .clear, lineWidth: 2)
            )
            .overlay(alignment: .topTrailing) {
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.title3)
                        .symbolRenderingMode(.palette)
                        .foregroundStyle(.black, Color.accentColor)
                        .padding(14)
                }
            }
        }
        .buttonStyle(PressableStyle())
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}
