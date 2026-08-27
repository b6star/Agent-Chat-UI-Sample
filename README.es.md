# Ejemplo de AgentChatUI (AgentChatUI Sample)

Una **plantilla de interfaz de chat AI con Streaming** para Android, construida íntegramente con Jetpack Compose y lista para ser adaptada.
Incluye un backend simulado (mock) para que puedas probar la experiencia completa de chat de inmediato, y luego conectar cualquier **Provider** de LLM (Gemini, OpenAI, un modelo local, ...) implementando una única interfaz.

## Funciones

- **Respuestas en Streaming**: los **Providers** emiten (**Emit**) valores `Flow<ChatResponse>` (incluyendo `ChatResponse.Chunk`), que se renderizan en vivo con escrituras de persistencia aceleradas (250 ms).
- **Renderizado de Markdown**: negrita, cursiva, código inline, encabezados, listas, citas, enlaces y vistas previas de imágenes.
- **Bloques de código**: resaltado de sintaxis mediante highlight.js y renderizado de **diagramas Mermaid** dentro de un WebView, ambos respaldados por una caché de renderizado LRU.
- **Gestión de sesiones**: panel lateral con chats recientes, nuevo chat, renombrar, eliminar y estadísticas por sesión (recuento de mensajes, tiempo medio de respuesta, tokens totales, coste estimado).
- **Metadatos de mensajes**: toca el icono de información de un mensaje para inspeccionar el **Provider**, el modelo, los tokens (prompt/completado/pensamientos), el tiempo de respuesta y la estimación de costes.
- **Adjuntos**: hasta 10 imágenes/archivos (20 MB en total) desde la galería, archivos o cámara.
- **Selector de modelo**: tres niveles de modelo más un limitador de longitud de historial (20 / 50 / mensajes ilimitados enviados como contexto).
- **Detener generación**: cancela un **Stream** en curso a mitad de la respuesta.
- **Comandos con barra**: `/help`, `/clear`, `/code`, ejemplos de lenguajes y guías jerárquicas `/project/...` con navegación clicable.
- **Marcadores de pensamiento**: renderiza marcadores de progreso `THINKING_STEP` antes de la respuesta final de la IA.
- **Localización**: inglés, coreano, chino simplificado, japonés, español, portugués brasileño, francés y alemán.

Sin dependencias de Room, Firebase, Hilt o Navigation — solo Compose + coroutines + serialization.

## Comandos con barra

Escribe `/` en el campo de entrada para ver sugerencias de comandos. Las sugerencias se filtran según el texto introducido después de la barra, y al seleccionar una se introduce el comando completo en el campo de entrada.

| Comando | Propósito |
|---|---|
| `/help` | Mostrar todos los comandos disponibles |
| `/clear` | Borrar el historial del chat actual |
| `/code` | Mostrar ejemplos básicos para cada lenguaje soportado |
| `/kotlin`, `/python`, `/java` | Mostrar un ejemplo básico del lenguaje |
| `/kotlin-long`, `/python-very-long`, etc. | Mostrar variantes de código más largas |
| `/mermaid` | Mostrar un ejemplo de diagrama de flujo Mermaid |
| `/project` | Abrir la guía de integración del proyecto |
| `/project/ai-connect/...` | Abrir un tema específico de conexión AI |

Las respuestas de los comandos con barra se abren en una sesión de explicación separada. Su pie de página de metadatos se oculta, y las rutas de comandos entre comillas simples en la documentación son clicables.

## Estructura del proyecto

```
com.b6star.chatui/
├─ App.kt                    # Entrada de la aplicación; inicializa ServiceLocator
├─ MainActivity.kt           # Aloja directamente AgentScreen
├─ ai/                       # ★ Punto de extensión — cambia solo esta capa
│  ├─ AiGateway.kt           #    Contrato del **Provider**: chatStream() -> Flow<ChatResponse>
│  ├─ ChatResponse.kt        #    Chunk | Metadata | ShowDetails
│  ├─ Attachment.kt          #    AiImageAttachment / AiFileAttachment
│  ├─ AiModelCatalog.kt      #    IDs de modelos mostrados en el selector
│  └─ MockAiGateway.kt       #    Implementación de demostración (Streaming de contenido de ejemplo)
├─ data/
│  ├─ model/ChatModels.kt    #    Clases de datos ChatMessage / ChatSession
│  └─ ChatRepository.kt      #    Almacén en memoria con una conversación de ejemplo
├─ di/ServiceLocator.kt      # Inyección manual de dependencias
├─ util/Utils.kt             # Ayudantes para estimación de costes y formato
└─ ui/                       # Pantalla, ViewModel, renderizador Markdown, WebView, diálogos, paleta
```

## Conectar un **Provider** de LLM real

1. Implementa `AiGateway`:

```kotlin
class GeminiGateway : AiGateway {
    override fun chatStream(
        history: List<ChatMessage>,
        model: String,
        historyLimit: Int,
        images: List<AiImageAttachment>,
        files: List<AiFileAttachment>
    ): Flow<ChatResponse> = flow {
        // Llama aquí a la API de Streaming de tu SDK y traduce cada delta en:
        emit(ChatResponse.Chunk(textDelta))
        // Opcionalmente, una vez al final (**Emit**):
        emit(ChatResponse.Metadata(promptTokens = ..., candidatesTokens = ...,
                                   responseTimeMs = ..., modelName = model, provider = "gemini"))
    }
}
```

2. Cambia una línea en `di/ServiceLocator.kt`:

```kotlin
val aiGateway: AiGateway by lazy { GeminiGateway() }   // antes era MockAiGateway()
```

Eso es todo — la interfaz de usuario, el pipeline de Streaming, la visualización de metadatos y la gestión de sesiones seguirán funcionando sin cambios.

### Contrato de Streaming y respuesta

`AiGateway.chatStream()` devuelve `Flow<ChatResponse>`. Un **Provider** debe convertir cada delta del SDK o **Chunk** de respuesta en `ChatResponse.Chunk(text)`, emitir (**Emit**) metadatos cuando la información de uso esté disponible, y volver a lanzar `CancellationException` para que la función de detener generación funcione correctamente.

El `MockAiGateway` incluido demuestra el mismo contrato con **Chunks** deterministas. Es intencionadamente un simulacro en memoria y no llama a un servicio de IA externo.

### Notas del contrato

- Emite (**Emit**) `Chunk` repetidamente durante la generación; el ViewModel los acumula y los guarda en el almacenamiento cada 250 ms.
- Emitir (**Emit**) `Metadata` antes de que termine el **Stream** rellena el pie de página de la burbuja (tokens, coste, latencia). Omítelo si tu **Provider** no suministra estadísticas de uso.
- Emite (**Emit**) elementos `ShowDetails` para activar el diálogo inline "[Ver detalles]".
- La cancelación (`stopGeneration()`) simplemente cancela la corrutina recolectora — limpia tu **Stream** ascendente (upstream flow) en consecuencia.
- El repositorio actual es en memoria (los datos se pierden al cerrar el proceso). Para persistir los datos, reemplaza el interior de `ChatRepository` con Room o DataStore manteniendo su API pública.

## Notas sobre Markdown y Mermaid

- Los ejemplos de código en los recursos de strings de Android usan `\u0020` para los espacios iniciales, ya que los espacios literales repetidos pueden colapsar durante el procesamiento de recursos y HTML.
- Las etiquetas de nodos Mermaid que contienen comillas usan `\u0022`, por ejemplo `A[\u0022Procesar bloques\u0022]`.
- El analizador Markdown personalizado admite código inline con una sola comilla invertida, como `` `Código Inline` ``. Evita envolver esa forma en un par adicional de comillas invertidas.
- `AgentMarkdown.kt` controla el análisis de Markdown, los símbolos de encabezados/listas, la detección de `THINKING_STEP`, las rutas clicables y el formato inline. `AgentWebView.kt` controla el resaltado de código, el renderizado de Mermaid y la caché de renderizado.

## Primeros pasos

**Requisitos:** Android Studio (última versión), JDK 17+, Android SDK 37

```bash
git clone <tu fork>
# abra la carpeta en Android Studio y ejecuta ▶
```

o desde la línea de comandos:

```bash
./gradlew assembleDebug
```

| Configuración | Valor |
|---|---|
| Paquete | `com.b6star.chatui` |
| minSdk / targetSdk | 26 / 37 |
| Kotlin / AGP | 2.2.10 / 9.3.2 |
| Compose BOM | 2026.02.01 |

## Notas

- highlight.js y Mermaid se cargan desde CDN en tiempo de ejecución, por lo que el renderizado de bloques de código necesita acceso a la red en el primer renderizado (los resultados se guardan en caché después).
- Los costes de tokens son estimaciones aproximadas calculadas en `util/Utils.kt` con precios de ejemplo — ajústalos según tu **Provider**.
