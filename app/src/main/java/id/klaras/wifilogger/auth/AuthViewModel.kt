package id.klaras.wifilogger.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepositoryContract
) : ViewModel() {
    val authState: StateFlow<AuthState> = authRepository.authState as StateFlow<AuthState>

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun signInWithGoogle(idToken: String?) {
        if (idToken.isNullOrBlank()) {
            _errorMessage.value = "Login gagal: token tidak tersedia."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message
                    ?: "Login gagal. Coba lagi."
            }
            _isBusy.value = false
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
