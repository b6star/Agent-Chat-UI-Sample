# AI 로딩 인디케이터 동작 변경 계획

사용자가 메시지를 보냈을 때 즉시 스트리밍을 시작하는 대신, 항상 2.5초의 대기 시간을 갖고 그동안 로딩 인디케이터를 표시하도록 변경합니다. 스트리밍이 시작되면 로딩 인디케이터를 숨깁니다.

## Proposed Changes

### [AgentViewModel](file:///C:/Users/baejunsung/AndroidStudioProjects/AgentChatUISample/app/src/main/java/com/b6star/chatui/viewmodel/AgentViewModel.kt)

#### [MODIFY] [AgentViewModel.kt](file:///C:/Users/baejunsung/AndroidStudioProjects/AgentChatUISample/app/src/main/java/com/b6star/chatui/viewmodel/AgentViewModel.kt)
- `sendMessage` 함수 내에 `delay(2500)`을 추가합니다.
- 대기 중에는 `_isLoading`을 `true`로, `_isStreaming`을 `false`로 설정합니다.
- 대기 후 스트리밍이 시작될 때 `_isLoading`을 `false`로, `_isStreaming`을 `true`로 설정합니다.

### [AgentScreen](file:///C:/Users/baejunsung/AndroidStudioProjects/AgentChatUISample/app/src/main/java/com/b6star/chatui/ui/AgentScreen.kt)

#### [MODIFY] [AgentScreen.kt](file:///C:/Users/baejunsung/AndroidStudioProjects/AgentChatUISample/app/src/main/java/com/b6star/chatui/ui/AgentScreen.kt)
- `ChatInputArea` 호출 시 `isLoading` 매개변수에 `isLoading || isStreaming` 값을 전달하여 대기 중과 스트리밍 중에 모두 "정지" 버튼이 활성화되도록 합니다.
- `ChatArea`는 기존 `isLoading`을 그대로 사용하여 2.5초 대기 중에만 `AiLoadingIndicator`를 표시하게 됩니다.

## Verification Plan

### Manual Verification
1. 앱을 실행하고 메시지를 전송합니다.
2. 약 2.5초 동안 하단에 `AiLoadingIndicator`가 돌아가는지 확인합니다.
3. 2.5초 후 인디케이터가 사라지고 AI 메시지 스트리밍이 시작되는지 확인합니다.
4. 대기 중과 스트리밍 중에 입력창의 전송 버튼이 'X'(중단) 버튼으로 유지되는지 확인합니다.
