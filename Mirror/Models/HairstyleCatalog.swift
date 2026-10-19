import Foundation

enum HairstyleCatalog {
    static let all: [Hairstyle] = [
        Hairstyle(
            id: "crop",
            name: "Textured Crop",
            tag: "Short · Low fade",
            length: .short,
            notes: [
                BarberNote(label: "Sides", value: "Low skin fade, blended"),
                BarberNote(label: "Top", value: "Point-cut for texture, matte finish"),
                BarberNote(label: "Fringe", value: "Short, pushed slightly forward")
            ],
            description: "a short textured crop: low skin fade on the sides and back, about 4 cm of choppy textured hair on top with a short fringe pushed slightly forward, matte finish",
            frontPath: "M48 114 Q44 62 100 54 Q156 62 152 114 Q146 90 130 84 Q118 94 100 88 Q82 96 68 86 Q54 94 48 114 Z",
            sidePath: "M60 118 Q54 64 104 56 Q146 52 154 100 Q140 88 124 90 Q110 84 100 92 Q90 100 86 116 Q74 106 60 118 Z"
        ),
        Hairstyle(
            id: "buzz",
            name: "Buzz Cut",
            tag: "Short · Clipper #2",
            length: .short,
            notes: [
                BarberNote(label: "All over", value: "Clipper guard #2 (6 mm)"),
                BarberNote(label: "Neckline", value: "Tapered, natural"),
                BarberNote(label: "Edges", value: "Clean line-up at the temples")
            ],
            description: "an even buzz cut, clipper guard number 2 (about 6 mm) all over, tapered natural neckline, clean line-up",
            frontPath: "M50 106 Q52 66 100 62 Q148 66 150 106 Q140 82 100 78 Q60 82 50 106 Z",
            sidePath: "M62 112 Q60 68 104 62 Q142 60 150 100 Q136 86 104 86 Q84 90 82 112 Q72 106 62 112 Z"
        ),
        Hairstyle(
            id: "quiff",
            name: "Classic Quiff",
            tag: "Medium · Volume on top",
            length: .medium,
            notes: [
                BarberNote(label: "Sides", value: "Scissor taper, about 2 cm"),
                BarberNote(label: "Top", value: "6–8 cm, longest at the front"),
                BarberNote(label: "Styling", value: "Blow-dried up and back, light hold")
            ],
            description: "a classic quiff: tapered sides about 2 cm, 6 to 8 cm on top, longest at the front, blow-dried up and swept back with volume",
            frontPath: "M50 112 Q44 70 70 50 Q100 28 140 44 Q160 60 150 112 Q144 84 128 80 Q104 74 80 82 Q60 88 50 112 Z",
            sidePath: "M60 118 Q52 70 92 50 Q130 34 158 70 Q152 92 146 98 Q132 84 112 88 Q96 92 88 114 Q74 108 60 118 Z"
        ),
        Hairstyle(
            id: "part",
            name: "Side Part",
            tag: "Medium · Taper",
            length: .medium,
            notes: [
                BarberNote(label: "Sides", value: "Medium taper"),
                BarberNote(label: "Top", value: "5 cm, combed to one side"),
                BarberNote(label: "Part", value: "Natural part, not shaved")
            ],
            description: "a neat side part: medium taper on the sides, about 5 cm on top combed to one side along a natural part, slight shine",
            frontPath: "M48 116 Q44 64 98 56 Q152 60 152 116 Q148 86 120 80 L84 84 Q70 76 58 90 Q50 100 48 116 Z",
            sidePath: "M60 118 Q54 64 104 56 Q146 52 154 100 Q140 88 124 90 Q110 84 100 92 Q90 100 86 116 Q74 106 60 118 Z"
        ),
        Hairstyle(
            id: "curly",
            name: "Curly Top",
            tag: "Curly · Drop fade",
            length: .curly,
            notes: [
                BarberNote(label: "Sides", value: "Drop fade behind the ear"),
                BarberNote(label: "Top", value: "Keep curls, shape into a rounded crown"),
                BarberNote(label: "Styling", value: "Curl cream, no brushing")
            ],
            description: "a curly top with a drop fade: tight fade on the sides dropping behind the ear, defined natural curls on top shaped into a rounded crown",
            frontPath: "M48 112 Q40 80 58 70 Q60 52 80 54 Q90 40 108 46 Q126 40 136 56 Q156 58 152 80 Q160 96 152 112 Q140 90 100 86 Q60 90 48 112 Z",
            sidePath: "M60 118 Q52 70 92 50 Q130 34 158 70 Q152 92 146 98 Q132 84 112 88 Q96 92 88 114 Q74 108 60 118 Z"
        ),
        Hairstyle(
            id: "flow",
            name: "Flow",
            tag: "Long · Natural",
            length: .long,
            notes: [
                BarberNote(label: "Length", value: "Chin length, grown out evenly"),
                BarberNote(label: "Shape", value: "Light layers to remove bulk"),
                BarberNote(label: "Styling", value: "Tucked behind the ears, natural")
            ],
            description: "a men's flow haircut: chin-length hair grown out evenly with light layers, swept back and tucked behind the ears, natural texture",
            frontPath: "M44 146 Q36 58 100 50 Q164 58 156 146 Q152 104 138 92 Q118 82 100 84 Q80 82 62 92 Q50 104 44 146 Z",
            sidePath: "M58 142 Q50 60 104 52 Q150 50 156 104 Q142 90 126 92 Q110 86 100 94 Q88 104 86 118 Q72 112 58 142 Z"
        )
    ]

    static func style(id: String) -> Hairstyle? {
        all.first { $0.id == id }
    }

    /// Hair drawn on the empty-state illustrations (the "before" look).
    static let placeholderFront = "M44 140 Q36 58 100 50 Q164 58 156 140 Q152 104 138 92 Q118 100 100 90 Q80 102 62 92 Q50 104 44 140 Z"
    static let placeholderSide = "M58 142 Q50 60 104 52 Q150 50 156 104 Q142 90 126 92 Q110 86 100 94 Q88 104 86 118 Q72 112 58 142 Z"
}
