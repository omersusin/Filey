package filey.app.feature.organizer.model

data class OrganizerRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val extension: String,
    val targetPath: String,
    val isActive: Boolean = true
)
