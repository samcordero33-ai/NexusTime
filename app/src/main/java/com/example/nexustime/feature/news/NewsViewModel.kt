package com.example.nexustime.feature.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexustime.core.data.news.NewsItem
import com.example.nexustime.core.data.news.NewsRepository
import com.example.nexustime.core.service.tts.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado de la pantalla de noticias.
 *
 * `error` viaja separado de `items` para poder distinguir "no hay conexión" de
 * "el feed llegó vacío": son dos mensajes distintos para el usuario.
 */
data class NewsUiState(
    val loading: Boolean = false,
    val items: List<NewsItem> = emptyList(),
    val error: String? = null,
)

/**
 * ViewModel de "Noticias del día".
 *
 * La descarga es bloqueante ([NewsRepository]) y se hace en `Dispatchers.IO`.
 * La lectura en voz alta reutiliza [TtsManager]: no hace falta contexto propio,
 * así que basta con un [ViewModel] normal (no AndroidViewModel).
 */
class NewsViewModel : ViewModel() {

    private val _state = MutableStateFlow(NewsUiState(loading = true))
    val state: StateFlow<NewsUiState> = _state.asStateFlow()

    /** Nº de titulares que se leen en voz alta (los primeros). */
    private val maxSpoken = 5

    init {
        refresh()
    }

    /** Vuelve a descargar el feed. */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            val result = runCatching {
                withContext(Dispatchers.IO) { NewsRepository.fetchTopHeadlines() }
            }

            result.onSuccess { items ->
                _state.update {
                    it.copy(
                        loading = false,
                        items = items,
                        error = if (items.isEmpty()) "El feed no devolvió titulares" else null,
                    )
                }
            }.onFailure {
                _state.update {
                    it.copy(
                        loading = false,
                        error = "No se pudieron cargar las noticias. Revisa la conexión.",
                    )
                }
            }
        }
    }

    /**
     * Lee en voz alta los primeros titulares con [TtsManager].
     *
     * Se numeran para que se entienda el paso de uno a otro, ya que el motor de
     * voz encadena los textos sin pausas claras.
     */
    fun readAloud() {
        val headlines = _state.value.items.take(maxSpoken)
        if (headlines.isEmpty()) return

        val speech = buildString {
            append("Titulares del día. ")
            headlines.forEachIndexed { index, item ->
                append("${index + 1}. ${item.title}. ")
            }
        }
        TtsManager.speak(speech, flush = true)
    }

    /** Detiene la lectura en curso. */
    fun stopReading() {
        TtsManager.stop()
    }
}
