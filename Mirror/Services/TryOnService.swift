import UIKit

/// Turns the three scan photos into three photos wearing `style`.
/// All three go in one request so the model can keep the cut consistent across angles.
protocol TryOnService {
    func apply(style: Hairstyle, to images: [HeadAngle: UIImage]) async throws -> [HeadAngle: UIImage]
}

enum TryOnServiceFactory {
    static func make() -> TryOnService {
        if let endpoint = Config.tryOnEndpoint {
            return RemoteTryOnService(endpoint: endpoint)
        }
        return MockTryOnService()
    }
}

enum TryOnError: LocalizedError {
    case missingAngles
    case badResponse(Int)
    case undecodableImage(HeadAngle)

    var errorDescription: String? {
        switch self {
        case .missingAngles: "Take all three photos first."
        case .badResponse(let code): "The try-on server answered with an error (\(code)). Try again in a moment."
        case .undecodableImage(let angle): "The \(angle.label.lowercased()) photo came back damaged. Try again."
        }
    }
}

/// The instruction the image model receives. Kept here so the iOS app and the backend agree.
enum TryOnPrompt {
    static func make(for style: Hairstyle) -> String {
        """
        Edit only the hair of the person in these three photos of the same head \
        (front, right side, left side). Give them \(style.description). \
        The haircut must look identical across all three angles. \
        Keep the face, identity, skin, expression, ears, beard, clothing, lighting, \
        background and camera angle exactly as they are. Photorealistic, no illustration.
        """
    }
}

// MARK: - Mock (works offline, no key needed)

/// Returns the original photos with a label, after a short delay, so the whole
/// flow can be tested before the backend exists.
struct MockTryOnService: TryOnService {
    func apply(style: Hairstyle, to images: [HeadAngle: UIImage]) async throws -> [HeadAngle: UIImage] {
        guard HeadAngle.allCases.allSatisfy({ images[$0] != nil }) else { throw TryOnError.missingAngles }
        try await Task.sleep(nanoseconds: 1_500_000_000)
        var result: [HeadAngle: UIImage] = [:]
        for (angle, image) in images {
            result[angle] = Self.stamp(image.resized(maxSide: 1024), text: "Preview · \(style.name)")
        }
        return result
    }

    private static func stamp(_ image: UIImage, text: String) -> UIImage {
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        return UIGraphicsImageRenderer(size: image.size, format: format).image { context in
            image.draw(at: .zero)
            let fontSize = max(18, image.size.width * 0.04)
            let attributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: fontSize, weight: .semibold),
                .foregroundColor: UIColor.black
            ]
            let textSize = (text as NSString).size(withAttributes: attributes)
            let padding = fontSize * 0.6
            let badge = CGRect(
                x: padding,
                y: image.size.height - textSize.height - padding * 3,
                width: textSize.width + padding * 2,
                height: textSize.height + padding
            )
            UIColor(named: "AccentColor")?.setFill()
            UIBezierPath(roundedRect: badge, cornerRadius: badge.height / 2).fill()
            (text as NSString).draw(
                at: CGPoint(x: badge.minX + padding, y: badge.minY + padding / 2),
                withAttributes: attributes
            )
            _ = context
        }
    }
}

// MARK: - Remote (your backend)

/// Talks to your own backend, which holds the AI key. Contract:
///
/// POST `endpoint`   Content-Type: application/json
/// { "styleId": "crop", "prompt": "...", "images": { "front": "<base64 jpeg>", "right": "...", "left": "..." } }
///
/// 200 → { "images": { "front": "<base64 jpeg>", "right": "...", "left": "..." } }
struct RemoteTryOnService: TryOnService {
    let endpoint: URL
    var session: URLSession = .shared

    private struct RequestBody: Encodable {
        let styleId: String
        let prompt: String
        let images: [String: String]
    }

    private struct ResponseBody: Decodable {
        let images: [String: String]
    }

    func apply(style: Hairstyle, to images: [HeadAngle: UIImage]) async throws -> [HeadAngle: UIImage] {
        guard HeadAngle.allCases.allSatisfy({ images[$0] != nil }) else { throw TryOnError.missingAngles }

        var encoded: [String: String] = [:]
        for (angle, image) in images {
            encoded[angle.rawValue] = image.resized(maxSide: 1024)
                .jpegData(compressionQuality: 0.85)?
                .base64EncodedString()
        }

        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.timeoutInterval = 120
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(
            RequestBody(styleId: style.id, prompt: TryOnPrompt.make(for: style), images: encoded)
        )

        let (data, response) = try await session.data(for: request)
        let status = (response as? HTTPURLResponse)?.statusCode ?? 0
        guard (200..<300).contains(status) else { throw TryOnError.badResponse(status) }

        let body = try JSONDecoder().decode(ResponseBody.self, from: data)
        var result: [HeadAngle: UIImage] = [:]
        for angle in HeadAngle.allCases {
            guard let base64 = body.images[angle.rawValue],
                  let imageData = Data(base64Encoded: base64),
                  let image = UIImage(data: imageData) else {
                throw TryOnError.undecodableImage(angle)
            }
            result[angle] = image
        }
        return result
    }
}
