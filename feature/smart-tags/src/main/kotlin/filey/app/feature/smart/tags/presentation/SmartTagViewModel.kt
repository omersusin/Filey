package filey.app.feature.smart.tags.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import filey.app.feature.smart.tags.data.repository.SmartTagRepository
import filey.app.feature.smart.tags.domain.model.SmartTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SmartTagViewModel(
    private val repository: SmartTagRepository
) : ViewModel() {
    private val _tags = MutableStateFlow<List<SmartTag>>(emptyList())
    val tags = _tags.asStateFlow()

    fun loadTagsForFile(filePath: String) {
        viewModelScope.launch {
            _tags.value = repository.getTagsForFile(filePath)
        }
    }
}
