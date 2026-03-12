package filey.app.feature.server

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerUiState(
    val isRunning: Boolean = false,
    val ipAddress: String = "",
    val port: Int = 8080
) {
    val serverUrl: String get() = "http://$ipAddress:$port"
}

class ServerViewModel(context: Context) : ViewModel() {

    private val server = FileyServer(context)
    private val _uiState = MutableStateFlow(ServerUiState(ipAddress = server.getIpAddress()))
    val uiState: StateFlow<ServerUiState> = _uiState.asStateFlow()

    fun toggleServer() {
        if (_uiState.value.isRunning) {
            server.stop()
            _uiState.update { it.copy(isRunning = false) }
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isRunning = true) }
                server.start(_uiState.value.port)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        server.stop()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { ServerViewModel(context.applicationContext) }
        }
    }
}
