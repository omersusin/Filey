package filey.app.core.model

data class ClipboardData(
    val paths: List<String>,
    val isCut: Boolean // true = cut, false = copy
)
