# AgentChatUI Beispiel (AgentChatUI Sample)

Ein sofort einsatzbereites **Streaming AI-Chat-UI-Template** für Android, das vollständig mit Jetpack Compose erstellt wurde.
Es wird mit einem Mock-Backend geliefert, sodass Sie das vollständige Chat-Erlebnis sofort ausprobieren können. Durch Implementierung einer einzigen Schnittstelle können Sie jeden beliebigen LLM-**Provider** (Gemini, OpenAI, ein lokales Modell, ...) anbinden.

## Funktionen

- **Streaming-Antworten** — **Provider** emittieren (**Emit**) `Flow<ChatResponse>`-Werte (einschließlich `ChatResponse.Chunk`), die live mit gedrosselten Persistenz-Schreibvorgängen (250 ms) gerendert werden.
- **Markdown-Darstellung** — Fett, Kursiv, Inline-Code, Überschriften, Listen, Zitate, Links und Bildvorschauen.
- **Code-Blöcke** — Syntax-Hervorhebung über highlight.js und Rendern von **Mermaid-Diagrammen** innerhalb einer WebView, beides unterstützt durch einen LRU-Rendering-Cache.
- **Sitzungsverwaltung** — Seitliches Menü mit den letzten Chats, neuem Chat, Umbenennen, Löschen und Statistiken pro Sitzung (Nachrichtenanzahl, durchschnittliche Antwortzeit, Gesamt-Token, geschätzte Kosten).
- **Nachrichten-Metadaten** — Tippen Sie auf das Info-Symbol einer Nachricht, um **Provider**, Modell, Token (Prompt/Completion/Thoughts), Antwortzeit und Kostenschätzung einzusehen.
- **Anhänge** — Bis zu 10 Bilder/Dateien (insgesamt 20 MB) aus der Galerie, dem Dateisystem oder der Kamera.
- **Modell-Auswahl** — Drei Modell-Stufen sowie ein Begrenzer für die Verlaufslänge (20 / 50 / unbegrenzte Nachrichten, die als Kontext gesendet werden).
- **Generierung stoppen** — Brechen Sie einen laufenden **Stream** mitten in der Antwort ab.
- **Slash-Befehle** — `/help`, `/clear`, `/code`, Sprachbeispiele und hierarchische `/project/...`-Anleitungen mit anklickbarer Navigation.
- **Denkschritt-Markierungen** — Rendert `THINKING_STEP`-Fortschrittsmarkierungen vor der finalen AI-Antwort.
- **Lokalisierung** — Englisch, Koreanisch, vereinfachtes Chinesisch, Japanisch, Spanisch, brasilianisches Portugiesisch, Französisch und Deutsch.

Keine Abhängigkeiten von Room, Firebase, Hilt oder Navigation — nur Compose + Coroutines + Serialization.

## Slash-Befehle

Geben Sie `/` im Eingabefeld ein, um Befehlsvorschläge zu sehen. Die Vorschläge werden nach dem Text gefiltert, der nach dem Slash eingegeben wurde. Durch Auswahl eines Vorschlags wird der vollständige Befehl in das Eingabefeld übernommen.

| Befehl | Zweck |
|---|---|
| `/help` | Alle verfügbaren Befehle anzeigen |
| `/clear` | Aktuellen Chatverlauf löschen |
| `/code` | Grundbeispiele für jede unterstützte Sprache anzeigen |
| `/kotlin`, `/python`, `/java` | Grundbeispiel der Sprache anzeigen |
| `/kotlin-long`, `/python-very-long` usw. | Längere Code-Varianten anzeigen |
| `/mermaid` | Mermaid-Flussdiagramm-Beispiel anzeigen |
| `/project` | Integrationsleitfaden für das Projekt öffnen |
| `/project/ai-connect/...` | Spezifisches AI-Verbindungsthema öffnen |

Slash-Antworten werden in einer separaten Erklärungs-Sitzung geöffnet. Ihr Metadaten-Fußbereich wird ausgeblendet, und Slash-Pfade in Backticks innerhalb der Dokumentation sind anklickbar.

## Projektstruktur

```
com.b6star.chatui/
├─ App.kt                    # Anwendungseinstieg; initialisiert den ServiceLocator
├─ MainActivity.kt           # Hostet den AgentScreen direkt
├─ ai/                       # ★ Erweiterungspunkt — tauschen Sie nur diese Ebene aus
│  ├─ AiGateway.kt           #    **Provider**-Vertrag: chatStream() -> Flow<ChatResponse>
│  ├─ ChatResponse.kt        #    Chunk | Metadata | ShowDetails
│  ├─ Attachment.kt          #    AiImageAttachment / AiFileAttachment
│  ├─ AiModelCatalog.kt      #    Modell-IDs, die in der Auswahl angezeigt werden
│  └─ MockAiGateway.kt       #    Demo-Implementierung (Streaming von Beispielinhalten)
├─ data/
│  ├─ model/ChatModels.kt    #    ChatMessage / ChatSession Datenklassen
│  └─ ChatRepository.kt      #    In-Memory-Speicher, gefüllt mit einer Beispielkonversation
├─ di/ServiceLocator.kt      # Manuelle Dependency Injection
├─ util/Utils.kt             # Hilfsfunktionen für Kostenschätzung und Formatierung
└─ ui/                       # Screen, ViewModel, Markdown-Renderer, WebView, Dialoge, Palette
```

## Anbindung eines echten LLM-**Provider**s

1. Implementieren Sie `AiGateway`:

```kotlin
class GeminiGateway : AiGateway {
    override fun chatStream(
        history: List<ChatMessage>,
        model: String,
        historyLimit: Int,
        images: List<AiImageAttachment>,
        files: List<AiFileAttachment>
    ): Flow<ChatResponse> = flow {
        // Rufen Sie hier die Streaming-API Ihres SDKs auf und wandeln Sie jedes Delta um:
        emit(ChatResponse.Chunk(textDelta))
        // Optional, einmalig am Ende (**Emit**):
        emit(ChatResponse.Metadata(promptTokens = ..., candidatesTokens = ...,
                                   responseTimeMs = ..., modelName = model, provider = "gemini"))
    }
}
```

2. Ändern Sie eine Zeile in `di/ServiceLocator.kt`:

```kotlin
val aiGateway: AiGateway by lazy { GeminiGateway() }   // war MockAiGateway()
```

Das ist alles — UI, Streaming-Pipeline, Metadatenanzeige und Sitzungsverwaltung funktionieren unverändert weiter.

### Streaming- und Antwort-Vertrag

`AiGateway.chatStream()` liefert einen `Flow<ChatResponse>`. Ein **Provider** sollte jedes SDK-Delta oder jeden Antwort-**Chunk** in einen `ChatResponse.Chunk(text)` umwandeln, Metadaten emittieren (**Emit**), sobald Nutzungsinformationen verfügbar sind, und eine `CancellationException` erneut auslösen, damit das Stoppen der Generierung korrekt funktioniert.

Das mitgelieferte `MockAiGateway` demonstriert denselben Vertrag mit deterministischen **Chunk**s. Es handelt sich absichtlich um einen In-Memory-Mock, der keinen externen AI-Dienst aufruft.

### Vertragshinweise

- Emittieren (**Emit**) Sie während der Generierung wiederholt `Chunk`-Objekte. Das ViewModel sammelt diese und schreibt sie alle 250 ms in den Speicher.
- Das Emittieren (**Emit**) von `Metadata` vor dem Ende des **Stream**s füllt den Fußbereich der Sprechblase (Token, Kosten, Latenz). Lassen Sie dies weg, wenn Ihr **Provider** keine Nutzungsstatistiken liefert.
- Emittieren (**Emit**) Sie `ShowDetails`-Elemente, um den Inline-Dialog "[Details anzeigen]" zu ermöglichen.
- Das Abbrechen (`stopGeneration()`) bricht lediglich die sammelnde Coroutine ab — bereinigen Sie Ihren Upstream-**Stream** entsprechend.
- Das aktuelle Repository ist im Arbeitsspeicher (Daten gehen bei Prozessende verloren). Um Daten zu persistieren, ersetzen Sie das Innere von `ChatRepository` durch Room oder DataStore, während Sie die öffentliche API beibehalten.

## Hinweise zu Markdown und Mermaid

- Codebeispiele in Android-String-Ressourcen verwenden `\u0020` für führende Leerzeichen, da aufeinanderfolgende Leerzeichen während der Ressourcen- und HTML-Verarbeitung zusammengefasst werden können.
- Mermaid-Knotenbeschriftungen, die Anführungszeichen enthalten, verwenden `\u0022`, zum Beispiel `A[\u0022Blöcke parsen\u0022]`.
- Der benutzerdefinierte Markdown-Parser unterstützt Inline-Code mit einfachem Backtick, z. B. `` `Inline-Code` ``. Vermeiden Sie es, diese Form in ein zusätzliches Paar Backticks einzuschließen.
- `AgentMarkdown.kt` steuert das Markdown-Parsing, Überschriften-/Listen-Symbole, die `THINKING_STEP`-Erkennung, anklickbare Slash-Pfade und Inline-Formatierung. `AgentWebView.kt` steuert die Code-Hervorhebung, das Mermaid-Rendering und den Rendering-Cache.

## Erste Schritte

**Anforderungen:** Android Studio (aktuellste Version), JDK 17+, Android SDK 37

```bash
git clone <dein fork>
# Ordner in Android Studio öffnen und ausführen ▶
```

oder über die Kommandozeile:

```bash
./gradlew assembleDebug
```

| Konfiguration | Wert |
|---|---|
| Paket | `com.b6star.chatui` |
| minSdk / targetSdk | 26 / 37 |
| Kotlin / AGP | 2.2.10 / 9.3.2 |
| Compose BOM | 2026.02.01 |

## Hinweise

- highlight.js und Mermaid werden zur Laufzeit von einem CDN geladen. Daher benötigt das Rendern von Code-Blöcken beim ersten Mal einen Internetzugang (Ergebnisse werden danach zwischengespeichert).
- Die Token-Kosten sind grobe Schätzungen, die in `util/Utils.kt` mit Platzhalterpreisen berechnet werden — passen Sie diese entsprechend Ihrem **Provider** an.
