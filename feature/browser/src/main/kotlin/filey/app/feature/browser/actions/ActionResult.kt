package filey.app.feature.browser.actions

/**
 * Strongly-typed results from FileAction.execute().
 * Replaces the old "RENAME_REQUESTED:path" string hack.
 */
sealed interface ActionResult {
    data class Success(val message: String) : ActionResult
    data class Error(val message: String) : ActionResult
    data class RequestDelete(val path: String) : ActionResult
    data class RequestRename(val path: String) : ActionResult
    data class RequestProperties(val path: String) : ActionResult
    data object Dismissed : ActionResult
}
