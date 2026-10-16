package com.example.nexustime.core.data.news

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Titular normalizado para la UI de noticias.
 *
 * Solo se conserva lo que se muestra y se lee en voz alta: título, enlace y
 * fecha. El HTML del `description` del feed no aporta nada a un reloj de mesa y
 * ensuciaría el parser.
 */
data class NewsItem(
    val title: String,
    val link: String,
    val publishedAt: String?,
)

/**
 * Lector de titulares de un feed RSS.
 *
 * Se implementa con `HttpURLConnection` + `XmlPullParser` (ambos en el SDK) en
 * lugar de añadir Retrofit/OkHttp y un parser XML extra: para leer un único feed
 * la dependencia no se justifica, y el parser *pull* es de streaming, así que no
 * carga el XML entero en memoria.
 *
 * Es un `object` sin estado y **bloqueante**: el llamador (ViewModel) lo invoca
 * en `Dispatchers.IO`.
 */
object NewsRepository {

    private const val TAG = "NexusNews"

    /** BBC Mundo (RSS 2.0), fuente en español estable y sin API key. */
    private const val FEED_URL = "https://feeds.bbci.co.uk/mundo/rss.xml"

    private const val TIMEOUT_MS = 10_000
    private const val MAX_ITEMS = 15

    /**
     * Descarga y parsea los titulares.
     *
     * Lanza [java.io.IOException] si no hay red o el feed no responde; el
     * ViewModel lo traduce a un estado de error de la UI.
     */
    fun fetchTopHeadlines(): List<NewsItem> {
        val connection = (URL(FEED_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
            // Algunos CDN rechazan peticiones sin User-Agent.
            setRequestProperty("User-Agent", "NexusTime/1.1 (+Android)")
        }

        return try {
            val code = connection.responseCode
            Log.d(TAG, "Feed respondió HTTP $code")
            connection.inputStream.use { parse(it) }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Parser *pull* de los `<item>` del RSS 2.0.
     *
     * Se leen solo `title`, `link` y `pubDate`; el resto de etiquetas se ignoran.
     * Se corta a [MAX_ITEMS] porque en pantalla y en voz solo interesan los
     * primeros titulares.
     */
    private fun parse(input: InputStream): List<NewsItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        val items = mutableListOf<NewsItem>()
        var inItem = false
        var title: String? = null
        var link: String? = null
        var pubDate: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && items.size < MAX_ITEMS) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> {
                        inItem = true
                        title = null
                        link = null
                        pubDate = null
                    }

                    "title" -> if (inItem) title = parser.readText()
                    "link" -> if (inItem) link = parser.readText()
                    "pubDate" -> if (inItem) pubDate = parser.readText()
                }

                XmlPullParser.END_TAG -> if (parser.name == "item" && inItem) {
                    inItem = false
                    val cleanTitle = title?.trim()
                    if (!cleanTitle.isNullOrEmpty()) {
                        items += NewsItem(
                            title = cleanTitle,
                            link = link?.trim().orEmpty(),
                            publishedAt = pubDate?.trim(),
                        )
                    }
                }
            }
            event = parser.next()
        }

        Log.d(TAG, "Titulares parseados: ${items.size}")
        return items
    }

    /**
     * Lee el texto de la etiqueta actual hasta su cierre.
     *
     * Se evita `nextText()` a propósito: lanza excepción si la etiqueta contiene
     * sub-etiquetas (algunos feeds meten HTML en `title`), y eso tumbaría el
     * parseo de todo el feed. Aquí se acumulan los tramos de texto y se ignoran
     * los elementos anidados.
     */
    private fun XmlPullParser.readText(): String {
        val startDepth = depth
        val builder = StringBuilder()
        var event = next()
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.TEXT,
                XmlPullParser.CDSECT,
                XmlPullParser.ENTITY_REF,
                -> builder.append(text ?: "")

                XmlPullParser.END_TAG -> if (depth == startDepth) return builder.toString().trim()
            }
            event = next()
        }
        return builder.toString().trim()
    }
}
