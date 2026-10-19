import SwiftUI

/// The flat head drawings from the mockups, rendered from the same 200×240 path data.
struct HeadIllustration: View {
    enum Pose { case front, right, left }

    var pose: Pose
    var hairPath: String
    var hairColor: Color = .accentColor
    var skin: Color = Color(white: 0.39)
    var shadow: Color = Color(white: 0.33)
    var torso: Color = Color(white: 0.23)

    var body: some View {
        ZStack {
            switch pose {
            case .front:
                SVGPath("M30 240 Q40 205 80 203 L120 203 Q160 205 170 240 Z").fill(torso)
                SVGPath("M82 176 L82 206 L118 206 L118 176 Z").fill(skin)
                SVGEllipse(cx: 47, cy: 128, rx: 8, ry: 14).fill(shadow)
                SVGEllipse(cx: 153, cy: 128, rx: 8, ry: 14).fill(shadow)
                SVGEllipse(cx: 100, cy: 124, rx: 52, ry: 64).fill(skin)
                SVGPath(hairPath).fill(hairColor)
            case .right, .left:
                SVGPath("M30 240 Q40 205 80 203 L130 203 Q165 206 172 240 Z").fill(torso)
                SVGPath("M86 172 L84 206 L124 206 L122 180 Z").fill(skin)
                SVGPath("M62 120 Q60 70 104 60 Q144 56 150 104 L152 116 L160 134 Q162 139 154 141 L156 150 Q158 156 152 158 L150 168 Q146 182 128 182 L112 184 Q86 186 74 168 Q62 150 62 120 Z").fill(skin)
                SVGEllipse(cx: 96, cy: 126, rx: 9, ry: 14).fill(shadow)
                SVGPath(hairPath).fill(hairColor)
            }
        }
        .aspectRatio(200.0 / 240.0, contentMode: .fit)
        // The side drawing faces right; mirror it for the left side.
        .scaleEffect(x: pose == .left ? -1 : 1, y: 1)
        .accessibilityHidden(true)
    }
}

extension HeadIllustration {
    init(angle: HeadAngle, style: Hairstyle?, hairColor: Color = .accentColor) {
        let pose: Pose = switch angle {
        case .front: .front
        case .right: .right
        case .left: .left
        }
        let path: String
        if let style {
            path = angle == .front ? style.frontPath : style.sidePath
        } else {
            path = angle == .front ? HairstyleCatalog.placeholderFront : HairstyleCatalog.placeholderSide
        }
        self.init(pose: pose, hairPath: path, hairColor: hairColor)
    }
}

// MARK: - Path data in the mockups' 200×240 coordinate box

private let artboard = CGSize(width: 200, height: 240)

private func artboardTransform(in rect: CGRect) -> (scale: CGFloat, dx: CGFloat, dy: CGFloat) {
    let scale = min(rect.width / artboard.width, rect.height / artboard.height)
    let dx = rect.midX - artboard.width / 2 * scale
    let dy = rect.midY - artboard.height / 2 * scale
    return (scale, dx, dy)
}

/// A tiny SVG path reader for the absolute M, L, Q and Z commands used by the illustrations.
struct SVGPath: Shape {
    let d: String
    init(_ d: String) { self.d = d }

    func path(in rect: CGRect) -> Path {
        let (scale, dx, dy) = artboardTransform(in: rect)
        let tokens = Self.tokenize(d)
        var path = Path()
        var index = 0
        var command: Character = "M"

        func number() -> CGFloat {
            guard index < tokens.count else { return 0 }
            let value = Double(tokens[index]) ?? 0
            index += 1
            return CGFloat(value)
        }
        func point() -> CGPoint {
            let x = number()
            let y = number()
            return CGPoint(x: dx + x * scale, y: dy + y * scale)
        }

        while index < tokens.count {
            let token = tokens[index]
            if token.count == 1, let char = token.first, char.isLetter {
                command = char
                index += 1
                if command == "Z" || command == "z" {
                    path.closeSubpath()
                }
                continue
            }
            switch command {
            case "M":
                path.move(to: point())
                command = "L"
            case "L":
                path.addLine(to: point())
            case "Q":
                let control = point()
                let end = point()
                path.addQuadCurve(to: end, control: control)
            default:
                index += 1
            }
        }
        return path
    }

    private static func tokenize(_ d: String) -> [String] {
        var tokens: [String] = []
        var current = ""
        func flush() {
            if !current.isEmpty { tokens.append(current); current = "" }
        }
        for char in d {
            if char.isLetter {
                flush()
                tokens.append(String(char))
            } else if char == " " || char == "," || char == "\n" {
                flush()
            } else if char == "-" {
                flush()
                current.append(char)
            } else {
                current.append(char)
            }
        }
        flush()
        return tokens
    }
}

struct SVGEllipse: Shape {
    let cx: CGFloat, cy: CGFloat, rx: CGFloat, ry: CGFloat

    func path(in rect: CGRect) -> Path {
        let (scale, dx, dy) = artboardTransform(in: rect)
        let frame = CGRect(
            x: dx + (cx - rx) * scale,
            y: dy + (cy - ry) * scale,
            width: rx * 2 * scale,
            height: ry * 2 * scale
        )
        return Path(ellipseIn: frame)
    }
}
