# AgentChatUI 示例 (AgentChatUI Sample)

一个开箱即用的 **Streaming AI 聊天 UI 模板**，完全使用 Jetpack Compose 构建。
项目自带模拟后端，您可以直接运行并体验完整的聊天功能，然后通过实现单一接口连接任何 LLM **Provider**（如 Gemini、OpenAI、本地模型等）。

## 主要功能

- **Streaming 响应** — **Provider** **Emit** `Flow<ChatResponse>` 值（包括 `ChatResponse.Chunk`），通过节流持久化写入（250 毫秒）实现实时渲染。
- **Markdown 渲染** — 支持加粗、斜体、行内代码、标题、列表、引用、链接和图像预览。
- **代码块** — 通过 highlight.js 实现语法高亮，并在 WebView 中渲染 **Mermaid 图表**，两者均由 LRU 渲染缓存提供支持。
- **会话管理** — 侧边栏包含最近聊天、新聊天、重命名、删除功能，以及每个会话的统计数据（消息数、平均响应时间、总 Token、估算成本）。
- **消息元数据** — 点击消息的信息图标可查看 **Provider**、模型、Token（提示/完成/思考）、响应时间和成本估算。
- **附件** — 支持从相册、文件或相机添加最多 10 个图像/文件（总计 20 MB）。
- **模型选择器** — 三个模型层级加上历史长度限制器（20 / 50 / 无限制消息作为上下文发送）。
- **停止生成** — 在回答中途取消正在进行的 **Stream**。
- **斜杠命令** — `/help`、`/clear`、`/code`、语言示例，以及带有可点击导航的层级化 `/project/...` 指南。
- **思考步骤标记** — 在最终 AI 响应之前渲染 `THINKING_STEP` 进度标记。
- **本地化** — 支持英语、韩语、简体中文、日语、西班牙语、巴西葡萄牙语、法语和德语。

不依赖 Room、Firebase、Hilt 或 Navigation — 仅使用 Compose + Coroutines + Serialization。

## 斜杠命令

在输入框中输入 `/` 查看命令建议。建议会根据斜杠后输入的文本进行过滤，选择其中一个会将完整命令放入输入字段。

| 命令 | 用途 |
|---|---|
| `/help` | 显示所有可用命令 |
| `/clear` | 清除当前聊天记录 |
| `/code` | 显示每种支持语言的基础示例 |
| `/kotlin`, `/python`, `/java` | 显示基础语言示例 |
| `/kotlin-long`, `/python-very-long` 等 | 显示更长的代码变体 |
| `/mermaid` | 显示 Mermaid 流程图示例 |
| `/project` | 打开项目集成指南 |
| `/project/ai-connect/...` | 打开特定的 AI 连接主题 |

斜杠响应会在单独的说明会话中打开。它们的元数据页脚会被隐藏，文档中反引号包裹的斜杠路径是可点击的。

## 项目结构

```
com.b6star.chatui/
├─ App.kt                    # 应用程序入口；初始化 ServiceLocator
├─ MainActivity.kt           # 直接托管 AgentScreen
├─ ai/                       # ★ 扩展点 — 仅需更换此层
│  ├─ AiGateway.kt           #    **Provider** 契约：chatStream() -> Flow<ChatResponse>
│  ├─ ChatResponse.kt        #    Chunk | Metadata | ShowDetails
│  ├─ Attachment.kt          #    AiImageAttachment / AiFileAttachment
│  ├─ AiModelCatalog.kt      #    选择器中显示的模型 ID
│  └─ MockAiGateway.kt       #    演示实现（Streaming 传输示例内容）
├─ data/
│  ├─ model/ChatModels.kt    #    ChatMessage / ChatSession 数据类
│  └─ ChatRepository.kt      #    由示例对话填充的内存存储
├─ di/ServiceLocator.kt      # 手动依赖注入
├─ util/Utils.kt             # 成本估算和格式化助手
└─ ui/                       # 屏幕、ViewModel、Markdown 渲染器、WebView、对话框、调色板
```

## 连接真实 LLM **Provider**

1. 实现 `AiGateway`：

```kotlin
class GeminiGateway : AiGateway {
    override fun chatStream(
        history: List<ChatMessage>,
        model: String,
        historyLimit: Int,
        images: List<AiImageAttachment>,
        files: List<AiFileAttachment>
    ): Flow<ChatResponse> = flow {
        // 在此处调用 SDK 的 Streaming API 并将每个增量转换为：
        emit(ChatResponse.Chunk(textDelta))
        // 可选，在最后 **Emit** 一次：
        emit(ChatResponse.Metadata(promptTokens = ..., candidatesTokens = ...,
                                   responseTimeMs = ..., modelName = model, provider = "gemini"))
    }
}
```

2. 在 `di/ServiceLocator.kt` 中修改一行：

```kotlin
val aiGateway: AiGateway by lazy { GeminiGateway() }   // 之前是 MockAiGateway()
```

就这样 — UI、Streaming 管道、元数据显示和会话处理都将保持不变。

### Streaming 传输和响应契约

`AiGateway.chatStream()` 返回 `Flow<ChatResponse>`。**Provider** 应将每个 SDK 增量或响应 **Chunk** 转换为 `ChatResponse.Chunk(text)`，在用量信息可用时 **Emit** 元数据，并重新抛出 `CancellationException` 以确保停止生成功能正常工作。

内置的 `MockAiGateway` 使用确定的 **Chunk** 演示了相同的契约。它是一个内存模拟实现，不调用外部 AI 服务。

### 契约注意事项

- 在生成时重复 **Emit** `Chunk`；ViewModel 会累积它们并每 250 毫秒刷新到存储。
- 在 **Stream** 结束前 **Emit** `Metadata` 会填充气泡页脚（Token、成本、延迟）。如果您的 **Provider** 不提供用量统计，可以省略。
- **Emit** `ShowDetails` 项以驱动“[查看详情]”行内对话框。
- 取消（`stopGeneration()`）只是取消收集协程 — 请相应地清理您的上游 **Stream**。
- 当前存储库位于内存中（数据在进程结束时重置）。要持久化，请在保持公共 API 不变的情况下，将 `ChatRepository` 内部实现替换为 Room 或 DataStore。

## Markdown 和 Mermaid 注意事项

- 字符串资源中的代码示例使用 `\u0020` 表示前导空格，因为在资源和 HTML 处理期间，重复的字面空格可能会被合并。
- 包含引号的 Mermaid 节点标签使用 `\u0022`，例如 `A[\u0022解析块\u0022]`。
- 自定义 Markdown 解析器支持单反引号行内代码，如 `` `行内代码` ``。避免将该形式包裹在额外的一对反引号中。
- `AgentMarkdown.kt` 控制 Markdown 解析、标题/列表符号、`THINKING_STEP` 检测、可点击的斜杠路径和行内格式。`AgentWebView.kt` 控制代码高亮、Mermaid 渲染和渲染缓存。

## 开始使用

**要求：** Android Studio (最新版), JDK 17+, Android SDK 37

```bash
git clone <your fork>
# 在 Android Studio 中打开文件夹并运行 ▶
```

或从命令行：

```bash
./gradlew assembleDebug
```

| 配置 | 值 |
|---|---|
| 包名 | `com.b6star.chatui` |
| minSdk / targetSdk | 26 / 37 |
| Kotlin / AGP | 2.2.10 / 9.3.2 |
| Compose BOM | 2026.02.01 |

## 备注

- highlight.js 和 Mermaid 在运行时从 CDN 加载，因此首次渲染代码块时需要网络连接（之后结果会被缓存）。
- Token 成本是 `util/Utils.kt` 中使用占位价格计算的粗略估计 — 请根据您的 **Provider** 进行调整。
