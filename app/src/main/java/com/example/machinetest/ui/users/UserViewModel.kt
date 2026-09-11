package com.example.machinetest.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.machinetest.data.model.User
import com.example.machinetest.data.repository.UserRepository
import com.example.machinetest.utils.Constants
import com.example.machinetest.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    private var currentSkip = 0
    private val pageSize = Constants.PAGE_SIZE
    private var allLoadedUsers = mutableListOf<User>()
    private var searchJob: Job? = null

    init {
        observeNetworkChanges()
        loadInitialUsers()
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            networkUtils.observeNetworkStatus.collect { isAvailable ->
                val wasOffline = _uiState.value.isOffline
                _uiState.update { it.copy(isOffline = !isAvailable) }

                // Auto-refresh when coming back online if we were previously offline or had an error
                if (isAvailable && (wasOffline || _uiState.value.errorMessage != null)) {
                    refresh()
                }
            }
        }
    }

    fun loadInitialUsers() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            val offline = repository.isOffline()
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isOffline = offline
                )
            }

            currentSkip = 0
            val result = repository.getUsers(limit = pageSize, skip = 0)
            result.onSuccess { response ->
                allLoadedUsers.clear()
                allLoadedUsers.addAll(response.users)
                currentSkip = response.users.size

                val isLast = currentSkip >= response.total || response.users.isEmpty()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        users = allLoadedUsers.toList(),
                        errorMessage = null,
                        isLastPage = isLast
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "Failed to load users."
                    )
                }
            }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        // Guard conditions against duplicate or invalid page loads
        if (currentState.isLoading || currentState.isLoadingMore ||
            currentState.isRefreshing || currentState.isLastPage ||
            currentState.searchQuery.isNotBlank()
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val result = repository.getUsers(limit = pageSize, skip = currentSkip)
            result.onSuccess { response ->
                val newUsers = response.users
                allLoadedUsers.addAll(newUsers)
                currentSkip += newUsers.size

                val isLast = currentSkip >= response.total || newUsers.isEmpty()
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        users = allLoadedUsers.toList(),
                        isLastPage = isLast
                    )
                }
            }.onFailure {
                // Keep the current list, just dismiss footer loader
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val offline = repository.isOffline()
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    isOffline = offline,
                    errorMessage = null
                )
            }

            currentSkip = 0
            val result = repository.getUsers(limit = pageSize, skip = 0)
            result.onSuccess { response ->
                allLoadedUsers.clear()
                allLoadedUsers.addAll(response.users)
                currentSkip = response.users.size

                val isLast = currentSkip >= response.total || response.users.isEmpty()
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        users = allLoadedUsers.toList(),
                        errorMessage = null,
                        isLastPage = isLast,
                        searchQuery = ""
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = if (allLoadedUsers.isEmpty()) throwable.localizedMessage else null
                    )
                }
            }
        }
    }

    fun searchUsers(query: String) {
        val trimmed = query.trim()
        _uiState.update { it.copy(searchQuery = trimmed) }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (trimmed.isEmpty()) {
                // Restore regular paginated list
                _uiState.update {
                    it.copy(
                        users = allLoadedUsers.toList(),
                        isLastPage = currentSkip >= allLoadedUsers.size
                    )
                }
                return@launch
            }

            // Debounce user keystrokes
            delay(300)

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.searchUsers(trimmed)
            result.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        users = response.users,
                        isLastPage = true // Search results are returned in full or treated as single page
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "Search failed."
                    )
                }
            }
        }
    }

    fun retry() {
        if (_uiState.value.searchQuery.isNotBlank()) {
            searchUsers(_uiState.value.searchQuery)
        } else {
            loadInitialUsers()
        }
    }
}
