# NexusTime v0.1

Reloj de mesa y dashboard inteligente para tablets Android en **orientación horizontal**.

> v0.1 es la base arquitectónica. **Todavía no** incluye motor de voz (TTS), chime,
> alarmas avanzadas ni clima: esos módulos llegan en versiones posteriores. Aquí
> el objetivo es que el esqueleto compile, sea modular y respete la paleta oficial.

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
├── NexusTimeApp.kt                      # Layout raíz: 3 zonas en horizontal
├── core/
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
    └── settings/                        # Ajustes (próximamente)
        └── SettingsScreen.kt
```

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

- **Sin Hilt (todavía).** No hay repositorios ni fuentes de datos que inyectar; añadir
  DI ahora sería infraestructura sin consumidores. Se incorporará cuando exista la
  capa de datos (v0.2).
- **Sin navegación con back stack.** Es un dashboard de una sola pantalla: el destino
  seleccionado se guarda en memoria, no hace falta NavHost.
- **Tema solo oscuro.** Un reloj de mesa se usa con luz baja; un fondo claro arruinaría
  el propósito del producto.
- **Reloj alineado al segundo real.** El ticker calcula los milisegundos que faltan
  para el próximo segundo en vez de dormir 1 s a ciegas, así no acumula deriva.
- **`tnum` en el display.** Sin números tabulares el reloj "salta" de un lado a otro
  al cambiar de dígito.
