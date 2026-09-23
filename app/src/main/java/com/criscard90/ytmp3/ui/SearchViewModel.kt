package com.criscard90.ytmp3.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.criscard90.ytmp3.youtube.Innertube
import com.criscard90.ytmp3.youtube.VideoSearchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Stato della schermata di ricerca. */
sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Error(val message: String) : SearchUiState
    data class Success(val items: List<VideoSearchItem>) : SearchUiState
}

class SearchViewModel : ViewModel() {

    var state: SearchUiState by mutableStateOf(SearchUiState.Idle)
        private set

    fun search(query: String) {
        val normalized = query.trim()
        if (normalized.isEmpty()) return

        state = SearchUiState.Loading
        viewModelScope.launch {
            state = try {
                val results = withContext(Dispatchers.IO) { Innertube.search(normalized) }
                SearchUiState.Success(results)
            } catch (t: Throwable) {
                SearchUiState.Error(t.message ?: "Errore durante la ricerca")
            }
        }
    }
}
