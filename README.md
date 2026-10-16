# NexusTime v1.0-release

Reloj de mesa y dashboard inteligente para tablets Android en **orientación horizontal**.

> Primera versión estable: base v0.1, **avisos parlantes de corriente y batería**
> (v0.3), **anuncio horario por voz** (v0.2) y **dashboard de 3 zonas con reloj
> neón y agenda** (v0.4). La **v1.0** migra las preferencias a **Jetpack
> DataStore**, añade **formato de hora configurable** (Sistema / 12 h / 24 h),
> **política de pantalla** (siempre / solo al cargar) y **protección anti
> burn-in** por micro-desplazamiento del reloj.
>
> La tarjeta "TU DÍA" lee los eventos de hoy del calendario del dispositivo.
> **Todavía no** incluye clima, alarmas avanzadas ni temporizador (v0.5).

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
├── MainActivity.kt                      # Actividad única (landscape, permiso de calendario, pantalla encendida)
├── NexusTimeApplication.kt              # Arranque del proceso: preferencias + motor TTS
├── NexusTimeApp.kt                      # Layout raíz: 3 zonas en horizontal
├── core/
│   ├── data/
│   │   ├── calendar/
│   │   │   └── CalendarManager.kt       # Lectura de eventos de hoy (READ_CALENDAR)
│   │   └── local/
│   │       └── NexusSettings.kt         # Preferencias sobre Jetpack DataStore
│   ├── service/
│   │   ├── tts/
│   │   │   └── TtsManager.kt            # Motor Text-to-Speech compartido (español)
│   │   ├── chime/
│   │   │   ├── ChimeScheduler.kt        # AlarmManager: próximo anuncio horario
│   │   │   ├── ChimeReceiver.kt         # Dispara el anuncio y reprograma
│   │   │   └── ChimeText.kt             # Hora en formato hablado
│   │   └── receivers/
│   │       └── PowerConnectionReceiver.kt  # Avisos de corriente y batería
│   └── ui/
│       ├── theme/
│       │   ├── Color.kt                 # Paleta oficial + colores de aura/glow
│       │   ├── Theme.kt                 # NexusTimeTheme + esquema Material 3
│       │   └── Type.kt                  # Escala tipográfica y estilos del reloj
│       ├── components/
│       │   ├── NexusNavRail.kt          # Barra lateral de navegación
│       │   ├── NexusPanel.kt            # Contenedor con borde plata / metal
│       │   ├── BurnInShift.kt           # Micro-desplazamiento anti burn-in
│       │   └── ComingSoon.kt            # Estado "próximamente"
│       └── navigation/
│           └── NexusDestination.kt      # Destinos: Reloj / Agenda / Ajustes
└── feature/
    ├── clock/                           # Reloj neón + indicadores de estado
    │   ├── ClockScreen.kt
    │   ├── ClockStatusIndicators.kt     # Batería / carga + estado del chime
    │   └── ClockViewModel.kt
    ├── agenda/                          # Panel "TU DÍA" (agenda real)
    │   ├── AgendaContent.kt             # Lista + estados compartidos
    │   ├── AgendaPanel.kt               # Panel derecho
    │   ├── AgendaScreen.kt              # Pantalla central de Agenda
    │   └── AgendaViewModel.kt
    └── settings/                        # Ajustes (alertas, chime, hora y pantalla)
        ├── SettingsScreen.kt
        └── SettingsViewModel.kt
```

## Anuncio horario / chime (v0.2)

`ChimeScheduler` agenda con `AlarmManager` la **próxima hora alineada al reloj**
(en punto o y media, según el intervalo) y `ChimeReceiver` la anuncia por voz y
reprograma la siguiente. Se usa una alarma de un único disparo que se rearma tras
cada aviso, en lugar de una repetitiva, para respetar el cambio de horario y no
acumular deriva.

- Intervalos configurables: **30** o **60** minutos (Ajustes → Anuncio horario).
- La hora se escribe en formato hablado ("Son las 2 en punto de la tarde") para
  que el motor de voz no la lea dígito a dígito.
- Plantilla editable: `NexusSettings` guarda `"Son las {hora}"` (el `{hora}` se
  sustituye por la hora hablada); su edición desde la UI llegará en una versión
  posterior.
- Al reiniciar el dispositivo o actualizar la app, `ChimeReceiver` vuelve a
  programar la alarma (`BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`).
- Si no hay permiso de alarmas exactas (Android 12+), cae a una alarma inexacta en
  vez de fallar: el aviso puede llegar con margen, pero no se pierde.

## Alertas de carga (v0.3)

`PowerConnectionReceiver` escucha broadcasts del sistema y emite la frase
correspondiente con `TtsManager`:

| Acción del sistema | Evento | Frase por defecto |
|---|---|---|
| `ACTION_POWER_CONNECTED` | Conexión a corriente | "Cargador conectado. Iniciando carga." |
| `ACTION_POWER_DISCONNECTED` | Desconexión | "Desconectado de la corriente." |
| `ACTION_BATTERY_CHANGED` | Batería ≤ 15% | "Batería baja. Conecta el cargador." |
| `ACTION_BATTERY_CHANGED` | Carga al 100% (conectada) | "Batería completa." |

`ACTION_BATTERY_CHANGED` es un *sticky broadcast* que llega al registrarse y en cada
cambio de batería; sobre él se detectan el nivel crítico (umbral por defecto 15%) y
la carga completa (`BATTERY_STATUS_FULL`). Se usa esta acción y no
`ACTION_BATTERY_LOW`/`ACTION_BATTERY_FULL` porque `Intent.ACTION_BATTERY_FULL` no
existe en el SDK y el umbral de `ACTION_BATTERY_LOW` lo fija el fabricante.

El receptor se registra **de forma dinámica** en `MainActivity` (`onStart`/`onStop`)
y solo mientras el interruptor **Ajustes → Alertas de Carga parlantes** está activo,
de modo que registro y desregistro siempre se compensan (sin fugas de memoria).
Se usa `ContextCompat.RECEIVER_NOT_EXPORTED`: las acciones las envía el *system UID*,
así que ninguna otra app puede suplantarlas.

Las frases viven en **Jetpack DataStore** con valores por defecto; su edición desde
la UI llegará en una versión posterior.

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
│ lateral│      Reloj / Agenda /        │   "TU DÍA"       │
│ 88 dp  │      Ajustes (transición)    │    340 dp        │
└────────┴──────────────────────────────┴──────────────────┘
```

- **(a) Barra lateral:** accesos Reloj / Agenda / Ajustes. Seleccionado en azul
  eléctrico, sin seleccionar en plata. El indicador activo se anima solo.
- **(b) Zona central:** cambia de destino con una transición suave (fade + slide
  horizontal, sentido según el orden de la barra). En Reloj: hora en blanco puro
  (128 sp), segundero en azul eléctrico, aura de neón, fecha y zona horaria.
- **(c) Panel derecho:** tarjeta "TU DÍA" siempre visible, con los eventos de hoy.

La orientación se fija en el manifiesto con `android:screenOrientation="sensorLandscape"`:
permite landscape y landscape invertido, nunca vertical.

## Dashboard y agenda (v0.4)

La pantalla se divide en tres zonas fluidas. El display del reloj lleva un
**aura de neón** en dos capas, ambas derivadas del celeste oficial (`#80D8FF`):

1. Un **degradado radial** (`Brush.radialGradient`) detrás de los dígitos, cuya
   opacidad respira entre 10% y 28% en ~2.8 s (`rememberInfiniteTransition`).
2. Un **`Shadow`** sobre el propio texto, que el framework rasteriza en todos los
   niveles de API soportados (a diferencia de `Modifier.blur()`, que requiere API 31+).

Debajo del display hay **indicadores en miniatura** (celeste): estado de
carga/batería (lee `ACTION_BATTERY_CHANGED` en tiempo real) y si el anuncio
horario está activo.

La tarjeta **"TU DÍA"** y la pantalla central de **Agenda** comparten un único
`AgendaViewModel` (misma instancia, porque `viewModel()` resuelve el
`ViewModelStoreOwner` del Activity). `CalendarManager` consulta los eventos de
hoy del calendario del dispositivo en `Dispatchers.IO` y la lista se refresca al
volver a `ON_RESUME`, de modo que tras conceder el permiso se rellena sola.

El permiso `READ_CALENDAR` se pide al arrancar. La UI distingue tres estados:
cargando, **sin permiso** (con su propio mensaje) y **vacío real** ("No hay
eventos programados para hoy"), para no mostrar un vacío engañoso.

En 12 h / 24 h el display respeta la preferencia del usuario
(Ajustes → Formato de hora); la opción "Sistema" sigue a
`DateFormat.is24HourFormat`. En 12 h se muestra el meridiano junto al segundero.

## v1.0-release

La primera versión estable consolida la persistencia y añade ajustes de uso diario
para un reloj de mesa:

### Preferencias sobre Jetpack DataStore

`NexusSettings` deja atrás `SharedPreferences` y pasa a **Jetpack DataStore
(Preferences)** (`androidx.datastore:datastore-preferences`), la fuente de verdad de
v1.0: escrituras atómicas y consistentes en disco.

DataStore es **asíncrono por diseño**, mientras que un `BroadcastReceiver.onReceive()`
es síncrono y de ventana muy corta. Por eso la API se parte en dos caras:

- **Reactiva (`StateFlow`)** para la UI (`powerAlertsEnabled`, `chimeEnabled`,
  `chimeInterval`, `hourFormat`, `screenOnMode`). Se construyen con `stateIn` sobre
  `dataStore.data` y son `lazy`, porque `initialize()` debe haber corrido antes de
  tocar el store.
- **Suspend** para los receptores y el planificador (`isPowerAlertsEnabled()`,
  `phrase()`, `chimeIntervalMinutes()`, …), leídos con `data.first()`.

Los receptores (`PowerConnectionReceiver`, `ChimeReceiver`) invocan las lecturas
suspend dentro de **`goAsync()`**: extiende la ventana de entrega del broadcast y
devuelve un `PendingResult` que se cierra con `finish()` al terminar la corrutina.

### Formato de hora configurable

`ClockViewModel` combina el *ticker* con `NexusSettings.hourFormat`
(**Sistema / 12 h / 24 h**); cambiar la opción en Ajustes refresca el display de
inmediato, sin esperar al siguiente segundo. En 12 h se muestra el meridiano.

### Política de pantalla (`FLAG_KEEP_SCREEN_ON`)

`MainActivity` decide si mantiene la pantalla encendida según `ScreenOnMode`:

- **Siempre** — la pantalla nunca se apaga con la app en primer plano.
- **Solo al cargar** — se mantiene encendida únicamente con el cargador puesto; el
  estado de carga se sigue con un receptor propio de `ACTION_BATTERY_CHANGED`
  (independiente de las alertas de voz).

`FLAG_KEEP_SCREEN_ON` no requiere el permiso `WAKE_LOCK`.

### Protección anti burn-in

Un reloj de mesa deja los mismos píxeles encendidos durante horas; sobre OLED eso
quema la imagen. `rememberBurnInOffset()` (en `core/ui/components/BurnInShift.kt`)
devuelve cada **2 minutos** un pequeño desplazamiento de hasta **3 dp**, recorriendo
las ocho direcciones alrededor del centro. `ClockScreen` lo aplica al bloque del
reloj con `Modifier.offset { ... }` (versión lambda: solo recalcula layout, no
fuerza recomposición).

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
