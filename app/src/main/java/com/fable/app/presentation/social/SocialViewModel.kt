package com.fable.app.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fable.app.domain.model.FriendRequest
import com.fable.app.domain.model.PublicReaderProfile
import com.fable.app.domain.model.ReaderActivity
import com.fable.app.domain.repository.SocialRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SocialUiState(
    val query: String = "",
    val searchResults: List<PublicReaderProfile> = emptyList(),
    val selectedProfile: PublicReaderProfile? = null,
    val incomingRequests: List<FriendRequest> = emptyList(),
    val outgoingRequests: List<FriendRequest> = emptyList(),
    val friends: List<PublicReaderProfile> = emptyList(),
    val activityFeed: List<ReaderActivity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
)

class SocialViewModel(
    private val userId: String,
    private val myProfile: PublicReaderProfile,
    private val repository: SocialRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var profileJob: Job? = null

    init {
        watch(repository.observeIncomingRequests(userId)) { state, requests -> state.copy(incomingRequests = requests) }
        watch(repository.observeOutgoingRequests(userId)) { state, requests -> state.copy(outgoingRequests = requests) }
        watch(repository.observeFriends(userId)) { state, friends -> state.copy(friends = friends) }
        watch(repository.observeActivityFeed(userId)) { state, feed -> state.copy(activityFeed = feed) }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query, error = null) }
        searchJob?.cancel()
        if (query.trim().removePrefix("@").length < 2) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            repository.searchReaders(query)
                .catch { error -> _uiState.update { it.copy(isLoading = false, error = error.userMessage()) } }
                .collect { results ->
                    _uiState.update { it.copy(searchResults = results.filterNot { profile -> profile.uid == userId }, isLoading = false, error = null) }
                }
        }
    }

    fun openProfile(uid: String) {
        profileJob?.cancel()
        _uiState.update { it.copy(selectedProfile = null, isLoading = true, error = null) }
        profileJob = viewModelScope.launch {
            repository.observeReaderProfile(uid)
                .catch { error -> _uiState.update { it.copy(isLoading = false, error = error.userMessage()) } }
                .collect { profile -> _uiState.update { it.copy(selectedProfile = profile, isLoading = false) } }
        }
    }

    fun clearSelectedProfile() = _uiState.update { it.copy(selectedProfile = null) }

    fun sendRequest(recipient: PublicReaderProfile) = perform("Friend request sent.") {
        repository.sendFriendRequest(myProfile, recipient)
    }

    fun accept(request: FriendRequest) = perform("Friend request accepted.") {
        repository.acceptFriendRequest(userId, request.senderId)
    }

    fun reject(request: FriendRequest) = perform("Friend request declined.") {
        repository.rejectFriendRequest(userId, request.senderId)
    }

    fun cancel(request: FriendRequest) = perform("Friend request cancelled.") {
        repository.cancelFriendRequest(userId, request.recipientId)
    }

    fun removeFriend(friend: PublicReaderProfile) = perform("Friend removed.") {
        repository.removeFriend(userId, friend.uid)
    }

    fun clearMessage() = _uiState.update { it.copy(message = null, error = null) }

    private fun <T> watch(
        flow: kotlinx.coroutines.flow.Flow<T>,
        updateState: (SocialUiState, T) -> SocialUiState,
    ) {
        viewModelScope.launch {
            flow.catch { error -> _uiState.update { it.copy(isLoading = false, error = error.userMessage()) } }
                .collect { value -> _uiState.update { state -> updateState(state, value).copy(isLoading = false, error = null) } }
        }
    }

    private fun perform(successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, message = null) }
            try {
                action()
                _uiState.update { it.copy(message = successMessage) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(error = error.userMessage()) }
            }
        }
    }

    private fun Throwable.userMessage() = message?.takeIf(String::isNotBlank)
        ?: "Social features are unavailable right now. Please try again."
}
