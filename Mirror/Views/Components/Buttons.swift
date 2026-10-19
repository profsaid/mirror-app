import SwiftUI

/// Full-width filled button, 50 pt tall, like the system's prominent style.
struct PrimaryButton: View {
    let title: String
    var systemImage: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let systemImage { Image(systemName: systemImage) }
                Text(title)
            }
            .font(.headline)
            .foregroundStyle(.black)
            .frame(maxWidth: .infinity, minHeight: 50)
            .background(Color.accentColor, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(PressableStyle())
    }
}

/// 44 pt round icon button on a secondary fill.
struct CircleIconButton: View {
    let systemName: String
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 17, weight: .semibold))
                .frame(width: 44, height: 44)
                .background(Color(.secondarySystemBackground), in: Circle())
        }
        .buttonStyle(PressableStyle())
        .accessibilityLabel(label)
    }
}

struct PressableStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.7 : 1)
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

/// Rounded thumbnail that shows a photo when there is one, or the illustration otherwise.
struct AngleThumbnail: View {
    let angle: HeadAngle
    var image: UIImage?
    var style: Hairstyle?
    var height: CGFloat = 120
    var cornerRadius: CGFloat = 12

    var body: some View {
        ZStack {
            Color(.tertiarySystemBackground)
            if let image {
                Image(uiImage: image).resizable().scaledToFill()
            } else {
                HeadIllustration(angle: angle, style: style, hairColor: style == nil ? Color(.systemGray2) : .accentColor)
                    .padding(.top, 8)
            }
        }
        .frame(height: height)
        .frame(maxWidth: .infinity)
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        .accessibilityElement()
        .accessibilityLabel("\(angle.title) photo")
    }
}
