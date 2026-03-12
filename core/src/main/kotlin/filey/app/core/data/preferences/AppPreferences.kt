package filey.app.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import filey.app.core.model.AccessMode
import filey.app.core.model.SortOption
import filey.app.core.model.ViewMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "settings")

class AppPreferences(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private object Keys {
        val ACCESS_MODE = intPreferencesKey("access_mode")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val LAST_PATH = stringPreferencesKey("last_path")
        val FAVORITES = stringPreferencesKey("favorites")
        val RECENTS = stringPreferencesKey("recents")
        val FILE_TAGS = stringPreferencesKey("file_tags")
    }

    private val _accessMode = MutableStateFlow(AccessMode.NORMAL)
    val accessModeFlow: StateFlow<AccessMode> = _accessMode.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewModeFlow: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NAME_ASC)
    val sortOptionFlow: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _showHidden = MutableStateFlow(false)
    val showHiddenFlow: StateFlow<Boolean> = _showHidden.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favoritesFlow: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _recents = MutableStateFlow<List<String>>(emptyList())
    val recentsFlow: StateFlow<List<String>> = _recents.asStateFlow()

    private val _lastPath = MutableStateFlow<String?>(null)
    val lastPathFlow: StateFlow<String?> = _lastPath.asStateFlow()

    private val _fileTags = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val fileTagsFlow: StateFlow<Map<String, Set<String>>> = _fileTags.asStateFlow()

    init {
        scope.launch {
            context.dataStore.data.collect { prefs ->
                _accessMode.value = AccessMode.entries.getOrElse(prefs[Keys.ACCESS_MODE] ?: 0) { AccessMode.NORMAL }
                _viewMode.value = try { ViewMode.valueOf(prefs[Keys.VIEW_MODE] ?: "LIST") } catch (_: Exception) { ViewMode.LIST }
                _sortOption.value = try { SortOption.valueOf(prefs[Keys.SORT_OPTION] ?: "NAME_ASC") } catch (_: Exception) { SortOption.NAME_ASC }
                _showHidden.value = prefs[Keys.SHOW_HIDDEN] ?: false
                _lastPath.value = prefs[Keys.LAST_PATH]
                _favorites.value = prefs[Keys.FAVORITES]?.split("|")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
                _recents.value = prefs[Keys.RECENTS]?.split("|")?.filter { it.isNotBlank() } ?: emptyList()

                val tagsMap = mutableMapOf<String, Set<String>>()
                prefs[Keys.FILE_TAGS]?.split("||")?.filter { it.contains("::") }?.forEach { entry ->
                    val parts = entry.split("::")
                    if (parts.size == 2) {
                        tagsMap[parts[0]] = parts[1].split(",").toSet()
                    }
                }
                _fileTags.value = tagsMap
            }
        }
    }

    suspend fun toggleTag(path: String, tag: String) {
        context.dataStore.edit { prefs ->
            val currentMap = _fileTags.value.toMutableMap()
            val currentTags = currentMap[path]?.toMutableSet() ?: mutableSetOf()
            if (currentTags.contains(tag)) currentTags.remove(tag) else currentTags.add(tag)
            
            if (currentTags.isEmpty()) currentMap.remove(path) else currentMap[path] = currentTags
            
            val serialized = currentMap.map { "${it.key}::${it.value.joinToString(",")}" }.joinToString("||")
            prefs[Keys.FILE_TAGS] = serialized
        }
    }

    suspend fun addRecent(path: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENTS]?.split("|")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            current.remove(path)
            current.add(0, path)
            prefs[Keys.RECENTS] = current.take(20).joinToString("|")
        }
    }

    suspend fun toggleFavorite(path: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES]?.split("|")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            if (current.contains(path)) current.remove(path) else current.add(path)
            prefs[Keys.FAVORITES] = current.joinToString("|")
        }
    }

    suspend fun setAccessMode(mode: AccessMode) { context.dataStore.edit { it[Keys.ACCESS_MODE] = mode.ordinal } }
    suspend fun setViewMode(mode: ViewMode) { context.dataStore.edit { it[Keys.VIEW_MODE] = mode.name } }
    suspend fun setSortOption(option: SortOption) { context.dataStore.edit { it[Keys.SORT_OPTION] = option.name } }
    suspend fun setShowHidden(show: Boolean) { context.dataStore.edit { it[Keys.SHOW_HIDDEN] = show } }
    suspend fun setLastPath(path: String) { context.dataStore.edit { it[Keys.LAST_PATH] = path } }
    fun getLastPath(): String? = _lastPath.value
}
