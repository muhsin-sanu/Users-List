package com.example.machinetest.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.machinetest.data.model.User
import com.example.machinetest.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserDetailViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserDetailUiState())
    val uiState: StateFlow<UserDetailUiState> = _uiState.asStateFlow()

    private var currentUserId: Int = -1

    fun setUser(user: User) {
        currentUserId = user.id
        _uiState.update { it.copy(user = user, isLoading = false, errorMessage = null) }
        // Also refresh details from API in background if needed
        loadUser(user.id, silent = true)
    }

    fun loadUser(userId: Int, silent: Boolean = false) {
        currentUserId = userId
        if (!silent) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }

        viewModelScope.launch {
            val result = repository.getUserById(userId)
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                if (!silent || _uiState.value.user == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.localizedMessage ?: "Failed to load user details."
                        )
                    }
                }
            }
        }
    }

    fun retry() {
        if (currentUserId != -1) {
            loadUser(currentUserId)
        }
    }
}
