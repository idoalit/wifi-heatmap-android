package id.klaras.wifilogger.auth

import kotlinx.coroutines.flow.StateFlow

interface AuthRepositoryContract {
    val authState: StateFlow<AuthState>

    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    fun signOut()
}

