# Exemplo AgentChatUI (AgentChatUI Sample)

Um **template de interface de chat AI com Streaming** para Android, pronto para ser adaptado e construído inteiramente com Jetpack Compose.
Ele vem com um backend simulado (mock) para que você possa executar a experiência completa de chat imediatamente e, em seguida, conectar qualquer **Provider** de LLM (Gemini, OpenAI, um modelo local, ...) implementando uma única interface.

## Recursos

- **Respostas em Streaming** — os **Providers** emitem (**Emit**) valores `Flow<ChatResponse>` (incluindo `ChatResponse.Chunk`), que são renderizados ao vivo com gravações de persistência otimizadas (250 ms).
- **Renderização de Markdown** — negrito, itálico, código inline, cabeçalhos, listas, citações, links e pré-visualizações de imagens.
- **Blocos de código** — realce de sintaxe via highlight.js e renderização de **diagramas Mermaid** dentro de um WebView, ambos apoiados por um cache de renderização LRU.
- **Gerenciamento de sessões** — painel lateral com chats recentes, novo chat, renomear, excluir e estatísticas por sessão (contagem de mensagens, tempo médio de resposta, total de tokens, custo estimado).
- **Metadados de mensagens** — toque no ícone de informações de uma mensagem para inspecionar o **Provider**, modelo, tokens (prompt/conclusão/pensamentos), tempo de resposta e estimativa de custo.
- **Anexos** — até 10 imagens/arquivos (20 MB no total) da galeria, arquivos ou câmera.
- **Seletor de modelo** — três níveis de modelo e um limitador de histórico (20 / 50 / mensagens ilimitadas enviadas como contexto).
- **Parar geração** — cancele um **Stream** em andamento no meio da resposta.
- **Comandos de barra** — `/help`, `/clear`, `/code`, exemplos de linguagens e guias hierárquicos `/project/...` com navegação clicável.
- **Marcadores de pensamento** — renderiza marcadores de progresso `THINKING_STEP` antes da resposta final da IA.
- **Localização** — inglês, coreano, chinês simplificado, japonês, espanhol, português do Brasil, francês e alemão.

Sem dependências de Room, Firebase, Hilt ou Navigation — apenas Compose + coroutines + serialization.

## Comandos de barra

Digite `/` no campo de entrada para ver sugestões de comandos. As sugestões são filtradas pelo texto inserido após a barra, e selecionar uma coloca o comando completo no campo de entrada.

| Comando | Propósito |
|---|---|
| `/help` | Mostrar todos os comandos disponíveis |
| `/clear` | Limpar o histórico do chat atual |
| `/code` | Mostrar exemplos básicos para cada linguagem suportada |
| `/kotlin`, `/python`, `/java` | Mostrar um exemplo básico da linguagem |
| `/kotlin-long`, `/python-very-long`, etc. | Mostrar variantes de código mais longas |
| `/mermaid` | Mostrar um exemplo de fluxograma Mermaid |
| `/project` | Abrir o guia de integração do projeto |
| `/project/ai-connect/...` | Abrir um tópico específico de conexão AI |

As respostas de comandos de barra abrem em uma sessão de explicação separada. O rodapé de metadados é ocultado, e os caminhos de barra entre crases na documentação são clicáveis.

## Estrutura do projeto

```
com.b6star.chatui/
├─ App.kt                    # Entrada da aplicação; inicializa ServiceLocator
├─ MainActivity.kt           # Hospeda o AgentScreen diretamente
├─ ai/                       # ★ Ponto de extensão — altere apenas esta camada
│  ├─ AiGateway.kt           #    Contrato do **Provider**: chatStream() -> Flow<ChatResponse>
│  ├─ ChatResponse.kt        #    Chunk | Metadata | ShowDetails
│  ├─ Attachment.kt          #    AiImageAttachment / AiFileAttachment
│  ├─ AiModelCatalog.kt      #    IDs de modelos mostrados no seletor
│  └─ MockAiGateway.kt       #    Implementação de demonstração (Streaming de conteúdo de exemplo)
├─ data/
│  ├─ model/ChatModels.kt    #    Classes de dados ChatMessage / ChatSession
│  └─ ChatRepository.kt      #    Armazenamento em memória com uma conversa de exemplo
├─ di/ServiceLocator.kt      # Injeção manual de dependências
├─ util/Utils.kt             # Auxiliares de estimativa de custo e formatação
└─ ui/                       # Tela, ViewModel, renderizador Markdown, WebView, diálogos, paleta
```

## Conectando um **Provider** de LLM real

1. Implemente `AiGateway`:

```kotlin
class GeminiGateway : AiGateway {
    override fun chatStream(
        history: List<ChatMessage>,
        model: String,
        historyLimit: Int,
        images: List<AiImageAttachment>,
        files: List<AiFileAttachment>
    ): Flow<ChatResponse> = flow {
        // Chame a API de Streaming do seu SDK aqui e traduza cada delta em:
        emit(ChatResponse.Chunk(textDelta))
        // Opcionalmente, uma vez ao final (**Emit**):
        emit(ChatResponse.Metadata(promptTokens = ..., candidatesTokens = ...,
                                   responseTimeMs = ..., modelName = model, provider = "gemini"))
    }
}
```

2. Altere uma linha em `di/ServiceLocator.kt`:

```kotlin
val aiGateway: AiGateway by lazy { GeminiGateway() }   // era MockAiGateway()
```

Isso é tudo — a UI, o pipeline de Streaming, a exibição de metadados e o gerenciamento de sessões continuam funcionando sem alterações.

### Contrato de Streaming e resposta

`AiGateway.chatStream()` retorna `Flow<ChatResponse>`. Um **Provider** deve converter cada delta do SDK ou **Chunk** de resposta em `ChatResponse.Chunk(text)`, emitir (**Emit**) metadados quando as informações de uso estiverem disponíveis e relançar `CancellationException` para que a interrupção da geração funcione corretamente.

O `MockAiGateway` incluído demonstra o mesmo contrato com **Chunks** determinísticos. É intencionalmente um mock em memória e não chama um serviço de IA externo.

### Notas do contrato

- Emita (**Emit**) `Chunk` repetidamente durante a geração; o ViewModel os acumula e salva no armazenamento a cada 250 ms.
- Emitir (**Emit**) `Metadata` antes do fim do **Stream** preenche o rodapé do balão (tokens, custo, latência). Omita se o seu **Provider** não fornecer estatísticas de uso.
- Emita (**Emit**) itens `ShowDetails` para ativar o diálogo inline "[Ver detalhes]".
- O cancelamento (`stopGeneration()`) simplesmente cancela a corrutina coletora — limpe seu **Stream** upstream de acordo.
- O repositório atual é em memória (os dados são perdidos ao fechar o processo). Para persistir, substitua o interior do `ChatRepository` por Room ou DataStore, mantendo sua API pública.

## Notas sobre Markdown e Mermaid

- Exemplos de código nos recursos de strings do Android usam `\u0020` para espaços iniciais, pois espaços literais repetidos podem ser recolhidos durante o processamento de recursos e HTML.
- Rótulos de nós Mermaid que contêm aspas usam `\u0022`, por exemplo `A[\u0022Analisar blocos\u0022]`.
- O analisador Markdown personalizado suporta código inline com crase simples, como `` `Código Inline` ``. Evite envolver essa forma em um par adicional de crases.
- `AgentMarkdown.kt` controla a análise de Markdown, símbolos de cabeçalho/lista, detecção de `THINKING_STEP`, caminhos de barra clicáveis e formatação inline. `AgentWebView.kt` controla o realce de código, renderização Mermaid e cache de renderização.

## Primeiros passos

**Requisitos:** Android Studio (última versão), JDK 17+, Android SDK 37

```bash
git clone <seu fork>
# abra a pasta no Android Studio e execute ▶
```

ou pela linha de comando:

```bash
./gradlew assembleDebug
```

| Configuração | Valor |
|---|---|
| Pacote | `com.b6star.chatui` |
| minSdk / targetSdk | 26 / 37 |
| Kotlin / AGP | 2.2.10 / 9.3.2 |
| Compose BOM | 2026.02.01 |

## Notas

- highlight.js e Mermaid são carregados via CDN em tempo de execução, portanto, a renderização de blocos de código precisa de acesso à rede na primeira renderização (os resultados são armazenados em cache depois).
- Os custos de tokens são estimativas aproximadas calculadas em `util/Utils.kt` com preços de exemplo — ajuste de acordo com seu **Provider**.
