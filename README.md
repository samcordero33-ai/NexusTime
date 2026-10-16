# NexusTime v0.3

Reloj de mesa y dashboard inteligente para tablets Android en **orientación horizontal**.

> v0.3 añade los **avisos parlantes de corriente y batería** sobre la base v0.1.
> Para ello incorpora el motor de voz (`TtsManager`) y la capa de preferencias
> (`NexusSettings`). **Todavía no** incluye el chime horario (v0.2), la lectura de
> agenda/clima (v0.4) ni las alarmas avanzadas (v0.5).

## Stack

| Elemento | Versión |
|---|---|
| Kotlin | 1.9.22 |
| Android Gradle Plugin | 8.2.2 |
| Gradle | 8.2 |
| Compose Compiler | 1.5.8 |
| Compose BOM | 2024.02.00 |
| compileSdk / targetSdk | 34 |
| minSdk | 26 |
| JDK | 17 |

Las versiones viven en [`gradle/libs.versions.toml`](gradle/libs.versions.toml) (version catalog).

## Estructura de paquetes

```
com.example.nexustime/
├── MainActivity.kt                      # Actividad única (landscape, pantalla siempre encendida)
├── NexusTimeApplication.kt              # Arranque del proceso: preferencias + motor TTS
├── NexusTimeApp.kt                      # Layout raíz: 3 zonas en horizontal
├── core/
│   ├── data/
│   │   └── local/
│   │       └── NexusSettings.kt         # Preferencias (interruptor de alertas + frases)
│   ├── service/
│   │   ├── tts/
│   │   │   └── TtsManager.kt            # Motor Text-to-Speech compartido (español)
│   │   └── receivers/
│   │       └── PowerConnectionReceiver.kt  # Avisos de corriente y batería
│   └── ui/
│       ├── theme/
│       │   ├── Color.kt                 # Paleta oficial
│       │   ├── Theme.kt                 # NexusTimeTheme + esquema Material 3
│       │   └── Type.kt                  # Escala tipográfica y estilos del reloj
│       ├── components/
│       │   ├── NexusNavRail.kt          # Barra lateral de navegación
│       │   ├── NexusPanel.kt            # Contenedor con borde plata / metal
│       │   └── ComingSoon.kt            # Estado "próximamente"
│       └── navigation/
│           └── NexusDestination.kt      # Destinos de la barra lateral
└── feature/
    ├── clock/                           # Reloj gigante (implementado)
    │   ├── ClockScreen.kt
    │   └── ClockViewModel.kt
    ├── agenda/                          # Panel "TU DÍA" (contenedor maquetado)
    │   └── AgendaPanel.kt
    └── settings/                        # Ajustes (alertas de carga)
        ├── SettingsScreen.kt
        └── SettingsViewModel.kt
```

## Alertas de carga (v0.3)

`PowerConnectionReceiver` escucha cuatro *protected broadcasts* del sistema y emite
la frase correspondiente con `TtsManager`:

| Acción del sistema | Frase por defecto |
|---|---|
| `ACTION_POWER_CONNECTED` | "Cargador conectado. Iniciando carga." |
| `ACTION_POWER_DISCONNECTED` | "Desconectado de la corriente." |
| `ACTION_BATTERY_LOW` | "Batería baja. Conecta el cargador." |
| `ACTION_BATTERY_FULL` | "Batería completa." |

El receptor se registra **de forma dinámica** en `MainActivity` (`onStart`/`onStop`)
y solo mientras el interruptor **Ajustes → Alertas de Carga parlantes** está activo,
de modo que registro y desregistro siempre se compensan (sin fugas de memoria).
Se usa `ContextCompat.RECEIVER_NOT_EXPORTED`: las cuatro acciones las envía el
* system UID*, así que ninguna otra app puede suplantarlas.

Las frases viven en `SharedPreferences` con valores por defecto; su edición desde la
UI llegará en una versión posterior.

## Paleta oficial

| Rol | Nombre | Hex |
|---|---|---|
| Background | Negro OLED | `#000000` |
| OnBackground / textos principales | Blanco Puro | `#FFFFFF` |
| Primary (acentos) | Azul Eléctrico | `#1E88E5` |
| Secondary (widgets y estado) | Celeste | `#80D8FF` |
| Surface (bordes y tarjetas) | Plata / Metal | `#C8C8C8` |

Los colores derivados (rellenos de tarjeta, texto secundario, separadores) son la
**plata oficial compuesta sobre el negro OLED**, es decir el mismo `#C8C8C8` con
distinta opacidad. Ningún composable declara colores literales.

## Layout horizontal

```
┌────────┬──────────────────────────────┬──────────────────┐
│  (a)   │           (b)                │       (c)        │
│  Barra │      Zona central            │   Panel derecho  │
│ lateral│      Reloj gigante           │   "TU DÍA"       │
│ 88 dp  │      weight(1f)              │    320 dp        │
└────────┴──────────────────────────────┴──────────────────┘
```

- **(a) Barra lateral:** accesos rápidos Reloj / Alarmas / Ajustes. Seleccionado en
  azul eléctrico, sin seleccionar en plata.
- **(b) Zona central:** hora en blanco puro (128 sp), segundero en azul eléctrico,
  acento azul bajo el display, fecha y zona horaria en celeste.
- **(c) Panel derecho:** contenedor con borde plata / metal preparado para la agenda.

La orientación se fija en el manifiesto con `android:screenOrientation="sensorLandscape"`:
permite landscape y landscape invertido, nunca vertical.

## Compilar

```bash
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

En cada push a `main`, GitHub Actions compila y publica el artefacto `NexusTime-APK`.

## Decisiones de diseño

- **Sin Hilt (todavía).** La capa de datos (v0.3) es un singleton de preferencias
  sin dependencias que inyectar; añadir DI ahora sería infraestructura sin
  consumidores reales. Se incorporará cuando existan repositorios y fuentes de
  datos propiamente dichas.
- **Sin navegación con back stack.** Es un dashboard de una sola pantalla: el destino
  seleccionado se guarda en memoria, no hace falta NavHost.
- **Tema solo oscuro.** Un reloj de mesa se usa con luz baja; un fondo claro arruinaría
  el propósito del producto.
- **Reloj alineado al segundo real.** El ticker calcula los milisegundos que faltan
  para el próximo segundo en vez de dormir 1 s a ciegas, así no acumula deriva.
- **`tnum` en el display.** Sin números tabulares el reloj "salta" de un lado a otro
  al cambiar de dígito.
