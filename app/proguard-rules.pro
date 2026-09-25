# Reglas de ofuscacion/minificacion de NexusTime.
# En v0.1 la build de release no minifica (isMinifyEnabled = false).
# Se conserva el archivo para cuando se habilite R8.

-keep class com.example.nexustime.core.ui.theme.** { *; }
