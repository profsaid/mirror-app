package com.saeed.mirror.model

/** The three photos of a head scan, named after the side of the head the camera sees. */
enum class HeadAngle(val label: String, val title: String, val instruction: String, val detail: String) {
    FRONT(
        label = "Front",
        title = "Front",
        instruction = "Look straight ahead",
        detail = "Chin level, hair dry and the way you usually wear it."
    ),
    RIGHT(
        label = "Right",
        title = "Right side",
        instruction = "Turn your head left",
        detail = "Keep turning until your right ear faces the camera. Shoulders stay still."
    ),
    LEFT(
        label = "Left",
        title = "Left side",
        instruction = "Turn your head right",
        detail = "Same again the other way, until your left ear faces the camera."
    );

    val key: String get() = name.lowercase()
}

enum class HairLength(val label: String) { SHORT("Short"), MEDIUM("Medium"), LONG("Long"), CURLY("Curly") }

data class BarberNote(val label: String, val value: String)

data class Hairstyle(
    val id: String,
    val name: String,
    val tag: String,
    val length: HairLength,
    val notes: List<BarberNote>,
    /** Plain-language description sent to the image model. */
    val description: String,
    /** Illustration paths in a 200×240 box (same art as the iOS app and mockups). */
    val frontPath: String,
    val sidePath: String
) {
    val barberText: String
        get() = name + "\n" + notes.joinToString("\n") { "${it.label}: ${it.value}" }
}

data class SavedLook(val id: String, val styleId: String, val createdAt: Long) {
    fun fileName(angle: HeadAngle) = "look-$id-${angle.key}.jpg"
}
