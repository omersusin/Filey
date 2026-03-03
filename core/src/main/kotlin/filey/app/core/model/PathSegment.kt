package filey.app.core.model

data class PathSegment(
    val name: String,
    val fullPath: String
)

fun pathToSegments(path: String): List<PathSegment> {
    val parts = path.split("/").filter { it.isNotEmpty() }
    val segments = mutableListOf<PathSegment>()
    var current = ""
    for (part in parts) {
        current = "$current/$part"
        segments.add(PathSegment(name = part, fullPath = current))
    }
    return segments
}
