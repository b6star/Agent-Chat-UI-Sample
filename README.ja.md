# AgentChatUI サンプル (AgentChatUI Sample)

Jetpack Compose で完全に構築された、Android 用の **Streaming AI チャット UI テンプレート**です。
モックバックエンドが付属しているため、すぐにチャット体験を試すことができます。また、単一のインターフェースを実装するだけで、任意の LLM **Provider**（Gemini、OpenAI、ローカルモデルなど）を接続できます。

## 主な機能

- **Streaming 応答** — **Provider** が `Flow<ChatResponse>` 値（`ChatResponse.Chunk` を含む）を **Emit** し、250ms 間隔のスロットリングされた永続化書き込みによりリアルタイムでレンダリングされます。
- **Markdown レンダリング** — 太字、斜体、インラインコード、見出し、リスト、引用、リンク、画像プレビューをサポート。
- **コードブロック** — highlight.js による構文ハイライトと WebView 内での **Mermaid ダイアグラム** のレンダリングをサポート。どちらも LRU レンダリングキャッシュによって高速化されています。
- **セッション管理** — 最近のチャット、新しいチャット、名前変更、削除機能、およびセッションごとの統計（メッセージ数、平均応答時間、総トークン数、推定コスト）を表示するサイドドロワーを搭載。
- **メッセージメタデータ** — メッセージの情報アイコンをタップして、**Provider**、モデル、トークン（Prompt/Completion/Thoughts）、応答時間、コスト見積もりを確認。
- **添付ファイル** — ギャラリー、ファイル、またはカメラから最大 10 個の画像/ファイル（合計 20 MB）を添付可能。
- **モデルセレクター** — 3 つのモデル階層と、コンテキストとして送信する履歴の長さ制限（20 / 50 / 無制限）を設定可能。
- **生成停止** — 実行中の **Stream** を途中でキャンセル可能。
- **スラッシュコマンド** — `/help`、`/clear`、`/code`、言語サンプル、およびクリック可能なナビゲーションを備えた階層的な `/project/...` ガイドを表示。
- **思考ステップマーカー** — 最終的な AI 応答の前に `THINKING_STEP` 進行状況マーカーをレンダリング。
- **ローカライズ** — 日本語、英語、韓国語、簡体字中国語、スペイン語、ブラジルポルトガル語、フランス語、ドイツ語に対応。

Room、Firebase、Hilt、Navigation などの依存関係はなく、Compose + Coroutines + Serialization のみで構成されています。

## スラッシュコマンド

入力欄に `/` を入力するとコマンドの候補が表示されます。候補はスラッシュの後に入力されたテキストでフィルタリングされ、選択すると入力フィールドにコマンド全体が挿入されます。

| コマンド | 用途 |
|---|---|
| `/help` | 利用可能なすべてのコマンドを表示 |
| `/clear` | 現在のチャット履歴を消去 |
| `/code` | サポートされているすべての言語の基本例を表示 |
| `/kotlin`, `/python`, `/java` | 基本的な言語例を表示 |
| `/kotlin-long`, `/python-very-long` など | 長いコード例を表示 |
| `/mermaid` | Mermaid フローチャートの例を表示 |
| `/project` | プロジェクト統合ガイドを開く |
| `/project/ai-connect/...` | 特定の AI 接続トピックを開く |

スラッシュコマンドの応答は別の説明セッションで開かれます。メタデータのフッターは非表示になり、ドキュメント内のバッククォートで囲まれたスラッシュパスはクリック可能です。

## プロジェクト構造

```
com.b6star.chatui/
├─ App.kt                    # アプリケーションエントリ; ServiceLocator を初期化
├─ MainActivity.kt           # AgentScreen を直接ホスト
├─ ai/                       # ★ 拡張ポイント — このレイヤーのみを交換
│  ├─ AiGateway.kt           #    **Provider** 契約: chatStream() -> Flow<ChatResponse>
│  ├─ ChatResponse.kt        #    Chunk | Metadata | ShowDetails
│  ├─ Attachment.kt          #    AiImageAttachment / AiFileAttachment
│  ├─ AiModelCatalog.kt      #    セレクターに表示されるモデル ID
│  └─ MockAiGateway.kt       #    デモ実装（サンプルコンテンツを Streaming）
├─ data/
│  ├─ model/ChatModels.kt    #    ChatMessage / ChatSession データクラス
│  └─ ChatRepository.kt      #    サンプル会話が格納されたインメモリリポジトリ
├─ di/ServiceLocator.kt      # 手動依存関係注入（Manual DI）
├─ util/Utils.kt             # コスト見積もりとフォーマットのヘルパー
└─ ui/                       # 画面、ViewModel、Markdown レンダラー、WebView、ダイアログなど
```

## 実際の LLM **Provider** への接続

1. `AiGateway` を実装します：

```kotlin
class GeminiGateway : AiGateway {
    override fun chatStream(
        history: List<ChatMessage>,
        model: String,
        historyLimit: Int,
        images: List<AiImageAttachment>,
        files: List<AiFileAttachment>
    ): Flow<ChatResponse> = flow {
        // SDK の Streaming API を呼び出し、各差分を以下に変換します：
        emit(ChatResponse.Chunk(textDelta))
        // オプションとして、最後に一度だけ **Emit**：
        emit(ChatResponse.Metadata(promptTokens = ..., candidatesTokens = ...,
                                   responseTimeMs = ..., modelName = model, provider = "gemini"))
    }
}
```

2. `di/ServiceLocator.kt` の 1 行を書き換えます：

```kotlin
val aiGateway: AiGateway by lazy { GeminiGateway() }   // 以前は MockAiGateway()
```

これだけで、UI、Streaming パイプライン、メタデータ表示、セッション管理はそのまま動作し続けます。

### Streaming と応答契約

`AiGateway.chatStream()` は `Flow<ChatResponse>` を返します。**Provider** は、各 SDK 差分または応答 **Chunk** を `ChatResponse.Chunk(text)` に変換して **Emit** し、使用量情報が利用可能な場合はメタデータを送信し、生成停止が正しく機能するように `CancellationException` を再スローする必要があります。

同梱されている `MockAiGateway` は、確定的な **Chunk** を用いた同じ契約の例を示しています。これはデモ用のインメモリリポジトリであり、外部の AI サービスは呼び出しません。

### 契約に関する注意点

- 生成中に `Chunk` を繰り返し **Emit** してください。ViewModel はそれらを蓄積し、250ms ごとにストレージに反映（flush）します。
- **Stream** が終了する前に `Metadata` を **Emit** すると、吹き出しのフッター（トークン、コスト、レイテンシ）が埋まります。**Provider** が統計を提供しない場合は省略可能です。
- `ShowDetails` 項目を **Emit** して、"[詳細を表示]" インラインダイアログ機能を有効にできます。
- 生成停止（`stopGeneration()`）は、収集しているコルーチンをキャンセルするだけです。それに合わせてアップストリームの **Stream** をクリーンアップしてください。
- 現在のリポジトリはインメモリです（プロセス終了時にデータがリセットされます）。永続化するには、パブリック API を維持したまま `ChatRepository` の内部を Room または DataStore に置き換えてください。

## Markdown と Mermaid に関する注意

- 文字列リソース内のコード例では、リソースや HTML 処理中に連続したスペースが削除されるのを防ぐため、先頭のスペースに `\u0020` を使用します。
- 引用符を含む Mermaid ノードラベルは `\u0022` を使用します（例： `A[\u0022パースブロック\u0022]`）。
- カスタム Markdown パーサーは、`` `インラインコード` `` のような単一バッククォートをサポートしています。この形式をさらにバッククォートで囲まないでください。
- `AgentMarkdown.kt` は、Markdown 解析、見出し/リスト記号、`THINKING_STEP` の検出、クリック可能なスラッシュパスなどを制御します。`AgentWebView.kt` は、コードのハイライト、Mermaid レンダリング、レンダリングキャッシュを管理します。

## はじめに

**要件:** Android Studio (最新版), JDK 17+, Android SDK 37

```bash
git clone <your fork>
# Android Studio でフォルダを開き、実行 ▶
```

またはコマンドラインから：

```bash
./gradlew assembleDebug
```

| 設定 | 値 |
|---|---|
| パッケージ | `com.b6star.chatui` |
| minSdk / targetSdk | 26 / 37 |
| Kotlin / AGP | 2.2.10 / 9.3.2 |
| Compose BOM | 2026.02.01 |

## 備考

- highlight.js と Mermaid は実行時に CDN からロードされるため、最初のコードブロック表示にはネットワークアクセスが必要です（結果はキャッシュされます）。
- トークンコストは `util/Utils.kt` で定義されたプレースホルダー価格に基づく概算です。**Provider** に合わせて調整してください。
