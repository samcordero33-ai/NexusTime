package com.example.nexustime.feature.news

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexustime.core.data.news.NewsItem
import com.example.nexustime.core.ui.components.NexusPanel
import com.example.nexustime.core.ui.theme.NexusBlack
import com.example.nexustime.core.ui.theme.NexusBlue
import com.example.nexustime.core.ui.theme.NexusBorder
import com.example.nexustime.core.ui.theme.NexusSkyBlue
import com.example.nexustime.core.ui.theme.NexusSurface
import com.example.nexustime.core.ui.theme.NexusTextSecondary
import com.example.nexustime.core.ui.theme.NexusTimeTheme
import com.example.nexustime.core.ui.theme.NexusWhite

/**
 * Punto de entrada de la pantalla central de Noticias.
 */
@Composable
fun NewsRoute(
    modifier: Modifier = Modifier,
    viewModel: NewsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NewsScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onReadAloud = viewModel::readAloud,
        modifier = modifier,
    )
}

/**
 * Zona central en el destino Noticias: titulares del día con lectura por voz.
 *
 * El botón de altavoz lee los primeros titulares con [com.example.nexustime.core.service.tts.TtsManager]
 * bajo demanda; "Actualizar" vuelve a descargar el feed.
 */
@Composable
fun NewsScreen(
    state: NewsUiState,
    onRefresh: () -> Unit,
    onReadAloud: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexusBlack)
            .padding(horizontal = 32.dp, vertical = 28.dp),
    ) {
        Text(
            text = "Noticias",
            style = MaterialTheme.typography.headlineMedium,
            color = NexusWhite,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Titulares del día",
            style = MaterialTheme.typography.bodyMedium,
            color = NexusTextSecondary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Altavoz: lee los primeros titulares en voz alta (acción principal).
            Button(
                onClick = onReadAloud,
                enabled = state.items.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexusBlue,
                    contentColor = NexusWhite,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Leer titulares")
            }

            OutlinedButton(
                onClick = onRefresh,
                enabled = !state.loading,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusSkyBlue),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Actualizar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NexusPanel(
            title = "Titulares",
            subtitle = "Fuente: BBC Mundo",
            headerAccent = NexusSkyBlue,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            NewsContent(state = state)
        }
    }
}

/** Decide qué mostrar: cargando, error, vacío o la lista de titulares. */
@Composable
private fun NewsContent(
    state: NewsUiState,
    modifier: Modifier = Modifier,
) {
    when {
        state.loading -> NewsStateMessage(
            text = "Cargando titulares…",
            modifier = modifier,
            loading = true,
        )

        state.error != null -> NewsStateMessage(
            text = state.error,
            modifier = modifier,
        )

        state.items.isEmpty() -> NewsStateMessage(
            text = "No hay titulares disponibles",
            modifier = modifier,
        )

        else -> NewsList(
            items = state.items,
            modifier = modifier,
        )
    }
}

/** Lista de titulares; cada fila abre el enlace en el navegador. */
@Composable
private fun NewsList(
    items: List<NewsItem>,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items = items, key = { it.title }) { item ->
            NewsRow(
                item = item,
                onClick = {
                    if (item.link.isNotBlank()) {
                        // openUri puede fallar si no hay navegador; no debe tumbar la UI.
                        runCatching { uriHandler.openUri(item.link) }
                    }
                },
            )
        }
    }
}

/** Fila de titular: número, título y fecha. */
@Composable
private fun NewsRow(
    item: NewsItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = NexusSurface, shape = shape)
            .border(width = 1.dp, color = NexusBorder, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Newspaper,
            contentDescription = null,
            tint = NexusBlue,
            modifier = Modifier.size(22.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = NexusWhite,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (!item.publishedAt.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.publishedAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = NexusTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Estado centrado (cargando / error / vacío). */
@Composable
private fun NewsStateMessage(
    text: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) {
                CircularProgressIndicator(color = NexusSkyBlue)
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Icon(
                    imageVector = Icons.Outlined.Newspaper,
                    contentDescription = null,
                    tint = NexusTextSecondary,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = NexusTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600)
@Composable
private fun NewsScreenPreview() {
    NexusTimeTheme {
        NewsScreen(
            state = NewsUiState(
                items = listOf(
                    NewsItem("Titular de ejemplo número uno", "https://example.com/1", "Hoy"),
                    NewsItem("Segundo titular de prueba", "https://example.com/2", "Hoy"),
                ),
            ),
            onRefresh = {},
            onReadAloud = {},
        )
    }
}
