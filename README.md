# AgentChatUI Sample

A ready-to-fork **streaming AI chat UI template** for Android, built entirely with Jetpack Compose.
It ships with a mock backend so you can run the full chat experience out of the box, then connect any LLM provider (Gemini, OpenAI, a local model, ...) by implementing a single interface.

## Features

- **Streaming responses** — providers emit `Flow<ChatResponse>` values (including `ChatResponse.Chunk`), which render live with throttled persistence writes (250 ms)
- **Markdown rendering** — bold, italic, inline code, headings, lists, quotes, links and image previews
- **Code blocks** — syntax highlighting via highlight.js and **Mermaid diagram** rendering inside WebView, both backed by an LRU render cache
- **Session management** — side drawer with recent chats, new chat, rename, delete, per-session stats (message count, avg response time, total tokens, estimated cost)
- **Message metadata** — tap a message's info icon to inspect provider, model, prompt/completion/thoughts tokens, response time, cost estimate
- **Attachments** — up to 10 images/files (20 MB total) from gallery, files, or camera
- **Model selector** — three model tiers plus a history-length limiter (20 / 50 / unlimited messages sent as context)
- **Stop generation** — cancel an in-flight stream mid-answer
- **Slash commands** — `/help`, `/clear`, `/code`, language examples, and hierarchical `/project/...` guides with clickable navigation
- **Thinking-step markers** — render `THINKING_STEP` progress markers before the final AI response
- **Localization** — English, Korean, Simplified Chinese, Japanese, Spanish, Brazilian Portuguese, French, and German

No Room, Firebase, Hilt or Navigation dependencies — just Compose + coroutines + serialization.

## Slash commands

Type `/` in the composer to see command suggestions. Suggestions are filtered by the text entered after the slash, and selecting one places the complete command in the input field.

| Command | Purpose |
|---|---|
| `/help` | Show all available commands |
| `/clear` | Clear the current chat history |
| `/code` | Show basic examples for every supported language |
| `/kotlin`, `/python`, `/java` | Show a basic language example |
| `/kotlin-long`, `/python-very-long`, etc. | Show longer language variants |
| `/mermaid` | Show a Mermaid flowchart example |
| `/project` | Open the project integration guide |
| `/project/ai-connect/...` | Open a specific AI connection topic |

Slash responses open in a separate explanation session. Their metadata footer is hidden, and backticked slash paths in the documentation are clickable.

## Project structure

```
com.b6star.chatui/
├─ App.kt                    # Application entry; initializes ServiceLocator
├─ MainActivity.kt           # Hosts AgentScreen directly
├─ ai/                       # ★ Fork point — swap this layer only
│  ├─ AiGateway.kt           #    Provider contract: chatStream() -> Flow<ChatResponse>
│  ├─ ChatResponse.kt        #    Chunk | Metadata | ShowDetails
│  ├─ Attachment.kt          #    AiImageAttachment / AiFileAttachment
│  ├─ AiModelCatalog.kt      #    Model ids shown in the selector
│  └─ MockAiGateway.kt       #    Demo implementation (streams sample content)
├─ data/
│  ├─ model/ChatModels.kt    #    ChatMessage / ChatSession data classes
│  └─ ChatRepository.kt      #    In-memory store seeded with a sample conversation
├─ di/ServiceLocator.kt      # Manual dependency wiring
├─ util/Utils.kt             # Cost estimation & formatting helpers
└─ ui/                       # Screen, ViewModel, markdown renderer, WebView, dialogs, palette
```

## Connecting a real LLM provider

1. Implement `AiGateway`:

```kotlin
class GeminiGateway : AiGateway {
    override fun chatStream(
        history: List<ChatMessage>,
        model: String,
        historyLimit: Int,
        images: List<AiImageAttachment>,
        files: List<AiFileAttachment>
    ): Flow<ChatResponse> = flow {
        // Call your SDK's streaming API here and translate each delta into:
        emit(ChatResponse.Chunk(textDelta))
        // Optionally, once at the end:
        emit(ChatResponse.Metadata(promptTokens = ..., candidatesTokens = ...,
                                   responseTimeMs = ..., modelName = model, provider = "gemini"))
    }
}
```

2. Swap one line in `di/ServiceLocator.kt`:

```kotlin
val aiGateway: AiGateway by lazy { GeminiGateway() }   // was MockAiGateway()
```

That's it — the UI, streaming pipeline, metadata display and session handling all keep working unchanged.

### Streaming and response contract

`AiGateway.chatStream()` returns `Flow<ChatResponse>`. A provider should convert each SDK delta or response chunk into `ChatResponse.Chunk(text)`, emit metadata when usage information is available, and rethrow `CancellationException` so stop-generation works correctly.

The bundled `MockAiGateway` demonstrates the same contract with deterministic chunks. It is intentionally an in-memory mock and does not call an external AI service.

### Contract notes

- Emit `Chunk` repeatedly while generating; the ViewModel accumulates them and flushes to storage every 250 ms.
- Emitting `Metadata` before the stream ends fills the bubble footer (tokens, cost, latency). Omit it if your provider doesn't supply usage stats.
- Emit `ShowDetails` items to power the "[View Details]" (see details) inline dialog.
- Cancellation (`stopGeneration()`) simply cancels the collecting coroutine — clean up your upstream flow accordingly.
- The current repository is in-memory (data resets on process death). To persist, replace `ChatRepository` internals with Room or DataStore while keeping its public API.

## Markdown and Mermaid notes

- Code examples in Android string resources use `\u0020` for leading spaces because repeated literal spaces can be collapsed during resource and HTML processing.
- Mermaid node labels that contain quotes use `\u0022`, for example `A[\u0022Parse blocks\u0022]`, to avoid lexical errors in the WebView renderer.
- The custom Markdown parser supports single-backtick inline code such as `` `Inline Code` ``. Avoid wrapping that form in an additional pair of backticks.
- `AgentMarkdown.kt` controls Markdown parsing, heading/list symbols, `THINKING_STEP` detection, clickable slash paths, and inline formatting. `AgentWebView.kt` controls code highlighting, Mermaid rendering, and the render cache.

## Getting started

**Requirements:** Android Studio (latest), JDK 17+, Android SDK 37

```
git clone <your fork>
# open the folder in Android Studio and run ▶
```

or from the command line:

```
./gradlew assembleDebug
```

| Config | Value |
|---|---|
| Package | `com.b6star.chatui` |
| minSdk / targetSdk | 26 / 37 |
| Kotlin / AGP | 2.2.10 / 9.3.2 |
| Compose BOM | 2026.02.01 |

Supported app locales are configured in `app/src/main/res/xml/locales_config.xml` and packaged through `androidResources.localeFilters` in `app/build.gradle.kts`.

## Notes

- highlight.js and Mermaid are loaded from CDN at runtime, so code-block rendering needs network access on first render (results are cached afterwards).
- Token costs are rough estimates computed in `util/Utils.kt` with placeholder pricing — adjust per your provider.
