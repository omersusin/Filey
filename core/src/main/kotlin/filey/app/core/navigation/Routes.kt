package filey.app.core.navigation

import android.net.Uri

object Routes {
    const val BROWSER = "browser"
    const val VIEWER  = "viewer/{encodedPath}"
    const val EDITOR  = "editor/{encodedPath}"
    const val PLAYER  = "player/{encodedPath}"
    const val ARCHIVE = "archive/{encodedPath}"

    fun viewer(path: String)  = "viewer/${Uri.encode(path)}"
    fun editor(path: String)  = "editor/${Uri.encode(path)}"
    fun player(path: String)  = "player/${Uri.encode(path)}"
    fun archive(path: String) = "archive/${Uri.encode(path)}"
}
