package filey.app.feature.browser.actions

import filey.app.core.model.FileModel

class ActionRegistry(
    private val actions: List<FileAction>
) {
    fun getActionsForFile(file: FileModel): List<FileAction> =
        actions.filter { action -> action.isVisible(file) }

    companion object {
        /** Creates the default registry with all built-in actions. */
        fun createDefault(): ActionRegistry = ActionRegistry(
            listOf(
                OpenWithAction(),
                CopyAction(),
                CutAction(),
                RenameAction(),
                ShareAction(),
                CompressAction(),
                DeleteAction(),
                PropertiesAction(),
                ChecksumAction()
            )
        )
    }
}
