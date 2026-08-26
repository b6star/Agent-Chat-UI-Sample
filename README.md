# AgentChatUI Sample

A ready-to-fork **streaming AI chat UI template** for Android, built entirely with Jetpack Compose.
It ships with a mock backend so you can run the full chat experience out of the box, then connect any LLM provider (Gemini, OpenAI, a local model, ...) by implementing a single interface.

## Features

- **Streaming responses** — tokens arrive as `Flow<ChatResponse.Chunk>` and render live, with throttled persistence writes (250 ms) to keep recomposition cheap
- **Markdown rendering** — bold, italic, inline code, headings, lists, quotes, links and image previews
- **Code blocks** — syntax highlighting via highlight.js and **Mermaid diagram** rendering inside WebView, both backed by an LRU render cache
- **Session management** — side drawer with recent chats, new chat, rename, delete, per-session stats (message count, avg response time, total tokens, estimated cost)
- **Message metadata** — tap a message's info icon to inspect provider, model, prompt/completion/thoughts tokens, response time, cost estimate
- **Attachments** — up to 10 images/files (20 MB total) from gallery, files, or camera
- **Model selector** — three model tiers plus a history-length limiter (20 / 50 / unlimited messages sent as context)
- **Stop generation** — cancel an in-flight stream mid-answer

No Room, Firebase, Hilt or Navigation dependencies — just Compose + coroutines + serialization.

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

### Contract notes

- Emit `Chunk` repeatedly while generating; the ViewModel accumulates them and flushes to storage every 250 ms.
- Emitting `Metadata` before the stream ends fills the bubble footer (tokens, cost, latency). Omit it if your provider doesn't supply usage stats.
- Emit `ShowDetails` items to power the "[자세히 보기]" (see details) inline dialog.
- Cancellation (`stopGeneration()`) simply cancels the collecting coroutine — clean up your upstream flow accordingly.
- The current repository is in-memory (data resets on process death). To persist, replace `ChatRepository` internals with Room or DataStore while keeping its public API.

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

## Notes

- highlight.js and Mermaid are loaded from CDN at runtime, so code-block rendering needs network access on first render (results are cached afterwards).
- Token costs are rough estimates computed in `util/Utils.kt` with placeholder pricing — adjust per your provider.
