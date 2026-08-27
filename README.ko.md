# AgentChatUI 샘플 (AgentChatUI Sample)

Jetpack Compose로 완전히 빌드된 Android용 **Streaming AI 채팅 UI 템플릿**입니다. 즉시 포크하여 사용할 수 있도록 설계되었습니다.
기본적으로 모의 백엔드(Mock backend)가 포함되어 있어 별도의 설정 없이도 전체 채팅 환경을 실행해 볼 수 있으며, 단일 인터페이스만 구현하면 어떤 LLM **Provider**(Gemini, OpenAI, 로컬 모델 등)든 손쉽게 연결할 수 있습니다.

## 주요 기능 (Features)

- **Streaming 응답** — **Provider**가 `Flow<ChatResponse>` 값( `ChatResponse.Chunk` 포함)을 **Emit**하며, 250ms 간격의 스로틀링(throttled) 영속성 쓰기를 통해 실시간으로 렌더링합니다.
- **마크다운 렌더링** — 굵게, 기울임꼴, 인라인 코드, 제목, 목록, 인용구, 링크 및 이미지 미리보기를 지원합니다.
- **코드 블록** — highlight.js를 통한 구문 강조(syntax highlighting) 및 WebView 내의 **Mermaid 다이어그램** 렌더링을 지원하며, 두 기능 모두 LRU 렌더 캐시로 성능을 최적화했습니다.
- **세션 관리** — 최근 채팅 목록, 새 채팅 생성, 이름 변경, 삭제 기능이 포함된 사이드 드로어를 제공합니다. 세션별 통계(메시지 수, 평균 응답 시간, 총 토큰 수, 예상 비용)도 확인할 수 있습니다.
- **메시지 메타데이터** — 메시지의 정보 아이콘을 탭하여 **Provider**, 모델명, 토큰 정보(Prompt/Completion/Thoughts), 응답 시간 및 예상 비용을 상세히 검토할 수 있습니다.
- **첨부 파일** — 갤러리, 파일 앱 또는 카메라를 통해 최대 10개의 이미지/파일(총 20MB)을 첨부할 수 있습니다.
- **모델 선택기** — 세 가지 모델 티어와 컨텍스트로 전송할 메시지 기록 제한 설정(20개 / 50개 / 무제한)을 지원합니다.
- **생성 중단** — 답변 도중 실시간 **Stream**을 중단할 수 있습니다.
- **슬래시 명령어** — `/help`, `/clear`, `/code`, 언어별 예제 및 클릭 가능한 탐색 기능이 포함된 계층적 `/project/...` 가이드를 제공합니다.
- **생각 단계 마커** — 최종 AI 응답 전에 `THINKING_STEP` 진행 상태 마커를 렌더링합니다.
- **다국어 지원** — 한국어, 영어, 중국어(간체), 일본어, 스페인어, 포르투갈어(브라질), 프랑스어, 독일어를 지원합니다.

Room, Firebase, Hilt 또는 Navigation 의존성 없이 Compose + Coroutines + Serialization만으로 가볍게 구성되었습니다.

## 슬래시 명령어 (Slash commands)

입력창에 `/`를 입력하면 명령어 제안이 표시됩니다. 슬래시 뒤에 입력한 텍스트로 제안이 필터링되며, 항목을 선택하면 입력 필드에 전체 명령어가 자동 입력됩니다.

| 명령어 | 용도 |
|---|---|
| `/help` | 사용 가능한 모든 명령어 표시 |
| `/clear` | 현재 채팅 기록 삭제 |
| `/code` | 지원되는 모든 언어의 기본 예제 표시 |
| `/kotlin`, `/python`, `/java` | 기본 언어 예제 표시 |
| `/kotlin-long`, `/python-very-long` 등 | 더 긴 코드 예제 표시 |
| `/mermaid` | Mermaid 플로우차트 예제 표시 |
| `/project` | 프로젝트 통합 가이드 열기 |
| `/project/ai-connect/...` | 특정 AI 연결 주제 열기 |

슬래시 응답은 별도의 설명 세션에서 열립니다. 메타데이터 푸터는 숨겨지며, 문서 내의 백틱으로 감싸진 슬래시 경로(`/...`)는 클릭 시 해당 문서로 바로 이동합니다.

## 프로젝트 구조 (Project structure)

```
com.b6star.chatui/
├─ App.kt                    # 애플리케이션 진입점; ServiceLocator 초기화
├─ MainActivity.kt           # AgentScreen을 직접 호스팅
├─ ai/                       # ★ 확장 포인트 — 이 레이어만 교체하세요
│  ├─ AiGateway.kt           #    **Provider** 계약: chatStream() -> Flow<ChatResponse>
│  ├─ ChatResponse.kt        #    Chunk | Metadata | ShowDetails
│  ├─ Attachment.kt          #    AiImageAttachment / AiFileAttachment
│  ├─ AiModelCatalog.kt      #    선택기에 표시될 모델 ID 정의
│  └─ MockAiGateway.kt       #    데모 구현체 (샘플 콘텐츠 Streaming)
├─ data/
│  ├─ model/ChatModels.kt    #    ChatMessage / ChatSession 데이터 클래스
│  └─ ChatRepository.kt      #    샘플 대화가 포함된 인 메모리 저장소
├─ di/ServiceLocator.kt      # 수동 의존성 주입 (Manual DI)
├─ util/Utils.kt             # 비용 추정 및 포맷팅 헬퍼
└─ ui/                       # 화면, ViewModel, 마크다운 렌더러, WebView, 다이얼로그 등
```

## 실제 LLM **Provider** 연결하기

1. `AiGateway`를 구현합니다:

```kotlin
class GeminiGateway : AiGateway {
    override fun chatStream(
        history: List<ChatMessage>,
        model: String,
        historyLimit: Int,
        images: List<AiImageAttachment>,
        files: List<AiFileAttachment>
    ): Flow<ChatResponse> = flow {
        // 여기에 SDK의 Streaming API를 호출하고 각 델타를 다음과 같이 변환하세요:
        emit(ChatResponse.Chunk(textDelta))
        
        // 선택 사항: 마지막에 한 번 메타데이터를 **Emit**합니다.
        emit(ChatResponse.Metadata(promptTokens = ..., candidatesTokens = ...,
                                   responseTimeMs = ..., modelName = model, provider = "gemini"))
    }
}
```

2. `di/ServiceLocator.kt`에서 한 줄을 수정합니다:

```kotlin
val aiGateway: AiGateway by lazy { GeminiGateway() }   // 기존: MockAiGateway()
```

이것으로 충분합니다. UI, Streaming 파이프라인, 메타데이터 표시 및 세션 처리는 변경 없이 그대로 작동합니다.

### Streaming 및 응답 계약

`AiGateway.chatStream()`은 `Flow<ChatResponse>`를 반환합니다. **Provider**는 각 SDK 델타 또는 응답 **Chunk**를 `ChatResponse.Chunk(text)`로 변환하여 **Emit**하고, 사용량 정보가 있는 경우 메타데이터를 전달해야 합니다. 또한 생성 중단 기능이 올바르게 작동하도록 `CancellationException`을 다시 던져야(rethrow) 합니다.

번결된 `MockAiGateway`는 동일한 계약을 통해 확정적인 **Chunk**를 Streaming하는 예시를 보여줍니다. 이는 데모용 인 메모리 모의 객체이며 외부 AI 서비스를 호출하지 않습니다.

### 계약 관련 주의사항

- 생성 중에 `Chunk`를 반복적으로 **Emit**하세요. ViewModel은 이를 수집하여 250ms마다 저장소에 반영(flush)합니다.
- **Stream**이 끝나기 전에 `Metadata`를 **Emit**하면 말풍선 하단에 정보(토큰, 비용, 지연 시간)가 채워집니다. **Provider**가 사용량 통계를 제공하지 않는 경우 생략할 수 있습니다.
- `ShowDetails` 항목을 **Emit**하여 "[자세히 보기]" 인라인 다이얼로그 기능을 활성화할 수 있습니다.
- 생성 중단(`stopGeneration()`)은 단순히 수집 중인 코루틴을 취소합니다. 상위 **Stream**(upstream flow)을 이에 맞춰 정리해 주세요.
- 현재 저장소는 인 메모리 방식입니다(프로세스 종료 시 데이터 초기화). 영구 저장이 필요한 경우 `ChatRepository` 내부를 Room 또는 DataStore로 교체하되 공개 API는 유지하세요.

## 마크다운 및 Mermaid 관련 참고 사항

- 문자열 리소스 내의 코드 예제는 선행 공백에 `\u0020`을 사용합니다. 이는 리소스 처리 과정에서 연속된 공백이 축소되는 것을 방지하기 위함입니다.
- 따옴표를 포함하는 Mermaid 노드 라벨은 `\u0022`를 사용합니다 (예: `A[\u0022블록 분석\u0022]`).
- 커스텀 마크다운 파서는 `` `인라인 코드` ``와 같은 단일 백틱을 지원합니다. 이 형식을 이중 백틱으로 감싸지 마세요.
- `AgentMarkdown.kt`는 마크다운 파싱, 제목/목록 기호, `THINKING_STEP` 감지 및 클릭 가능한 슬래시 경로 등을 제어합니다. `AgentWebView.kt`는 코드 하이라이팅, Mermaid 렌더링 및 캐시를 관리합니다.

## 시작하기 (Getting started)

**요구사항:** Android Studio (최신 버전), JDK 17+, Android SDK 37

```bash
git clone <your fork>
# Android Studio에서 폴더를 열고 실행 ▶
```

또는 명령줄에서 실행:

```bash
./gradlew assembleDebug
```

| 설정 | 값 |
|---|---|
| 패키지명 | `com.b6star.chatui` |
| minSdk / targetSdk | 26 / 37 |
| Kotlin / AGP | 2.2.10 / 9.3.2 |
| Compose BOM | 2026.02.01 |

## 참고 사항

- highlight.js 및 Mermaid는 런타임에 CDN에서 로드되므로, 첫 렌더링 시 네트워크 연결이 필요합니다 (이후 결과는 캐시됩니다).
- 토큰 비용은 `util/Utils.kt`에 정의된 임시 가격을 기준으로 계산된 대략적인 추정치입니다. **Provider**에 맞게 조정해 주세요.
