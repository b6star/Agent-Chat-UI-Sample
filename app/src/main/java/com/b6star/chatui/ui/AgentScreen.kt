package com.b6star.chatui.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraEnhance
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.b6star.chatui.R
import com.b6star.chatui.data.model.ChatMessage
import com.b6star.chatui.data.model.ChatSession
import com.b6star.chatui.ai.AiModelCatalog
import com.b6star.chatui.di.ServiceLocator
import com.b6star.chatui.ui.theme.AgentChatTheme
import com.b6star.chatui.ui.theme.AgentTheme
import com.b6star.chatui.util.Utils
import com.b6star.chatui.viewmodel.AgentViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun createChatImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "chat_images").apply { mkdirs() }
    val image = File.createTempFile("chat_", ".jpg", directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", image)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(
    viewModel: AgentViewModel = viewModel {
        AgentViewModel(ServiceLocator.context)
    }
) {
    val messages by viewModel.messages.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val totalTokens by viewModel.sessionTotalTokens.collectAsState()
    val totalCost by viewModel.sessionTotalCost.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedHistoryLimit by viewModel.selectedHistoryLimit.collectAsState()
    val chatInput by viewModel.chatInput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val detailItems by viewModel.detailItems.collectAsState()
    val sessionStats by viewModel.sessionStats.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    AgentChatTheme(themeType = selectedTheme) {
        val colors = AgentTheme.colors
        val context = LocalContext.current
        val listState = rememberLazyListState()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val keyboardController = LocalSoftwareKeyboardController.current

        var showDetailDialog by remember { mutableStateOf<ChatMessage?>(null) }
        var showDeleteConfirmDialog by remember { mutableStateOf<Int?>(null) }
        var showSessionInfoDialog by remember { mutableStateOf<Int?>(null) }
        var showRenameDialogFor by remember { mutableStateOf<Int?>(null) }
        var previewImageUrl by remember { mutableStateOf<String?>(null) }
        var showSettingsSheet by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState()

        LaunchedEffect(messages.size, messages.lastOrNull()?.content, isLoading) {
            if (messages.isNotEmpty() || isLoading) {
                delay(120)
                listState.animateScrollToItem(0)
            }
        }

        AgentPanel(
            drawerState = drawerState,
            sessions = sessions,
            currentSessionId = currentSessionId,
            onSessionClick = { sessionId ->
                viewModel.selectSession(sessionId)
                scope.launch { drawerState.close() }
            },
            onSessionInfoClick = { sessionId ->
                viewModel.loadSessionStats(sessionId)
                showSessionInfoDialog = sessionId
            },
            onCreateNewChat = {
                viewModel.createNewChat()
                scope.launch { drawerState.close() }
            },
            onSettingsClick = {
                showSettingsSheet = true
                scope.launch { drawerState.close() }
            }
        ) {
            Scaffold(
                containerColor = colors.background,
                topBar = {
                    AgentHeader(
                        sessions = sessions,
                        currentSessionId = currentSessionId,
                        totalTokens = totalTokens,
                        totalCost = totalCost,
                        messages = messages,
                        selectedModel = selectedModel,
                        selectedHistoryLimit = selectedHistoryLimit,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onModelSelected = { viewModel.setModel(it) },
                        onHistoryLimitSelected = { viewModel.setHistoryLimit(it) },
                        onDeleteSession = { showDeleteConfirmDialog = it }
                    )
                }
            ) { padding ->
                val density = LocalDensity.current
                var inputBarHeightPx by remember { mutableIntStateOf(0) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .imePadding()
                ) {
                    ChatArea(
                        modifier = Modifier.fillMaxSize(),
                        bottomContentPadding = with(density) { inputBarHeightPx.toDp() } + 8.dp,
                        messages = messages,
                        currentSessionId = currentSessionId,
                        isLoading = isLoading,
                        isStreaming = isStreaming,
                        listState = listState,
                        onInfoClick = { showDetailDialog = it },
                        onImageClick = { previewImageUrl = it },
                        onAskAi = { errorMessage ->
                            viewModel.sendMessage(
                                context.resources.getString(R.string.prompt_fix_mermaid, errorMessage)
                            )
                        },
                        onShowDetailsAtIndex = { msg, index ->
                            viewModel.showDetailsAtIndex(msg, index)
                        }
                    )

                    ChatInputArea(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onGloballyPositioned { inputBarHeightPx = it.size.height },
                        chatInput = chatInput,
                        isLoading = isLoading || isStreaming,
                        onChatInputChanged = { viewModel.onChatInputChanged(it) },
                        onSendMessage = { input, images, files ->
                            viewModel.sendMessage(input, images, files)
                        },
                        onStopGeneration = { viewModel.stopGeneration() },
                        keyboardController = keyboardController
                    )
                }
            }
        }

        // Dialogs & Sheets
        if (showDetailDialog != null) {
            MessageDetailDialog(
                message = showDetailDialog!!,
                onDismiss = { showDetailDialog = null }
            )
        }

        previewImageUrl?.let { imageUrl ->
            AlertDialog(
                onDismissRequest = { previewImageUrl = null },
                confirmButton = {},
                text = {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = stringResource(R.string.image_preview),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }

        if (showDeleteConfirmDialog != null) {
            val isAll = showDeleteConfirmDialog == -1
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = { Text(if (isAll) stringResource(R.string.delete_all_chats_title) else stringResource(R.string.delete_chat_title)) },
                text = { Text(if (isAll) stringResource(R.string.delete_all_chats_msg) else stringResource(R.string.delete_chat_msg)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (isAll) viewModel.clearChat()
                            else viewModel.deleteSession(showDeleteConfirmDialog!!)
                            showDeleteConfirmDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (showSessionInfoDialog != null) {
            sessions.find { it.id == showSessionInfoDialog }?.let { session ->
                SessionInfoDialog(
                    session = session,
                    stats = sessionStats,
                    onDelete = {
                        showSessionInfoDialog = null
                        showDeleteConfirmDialog = session.id
                    },
                    onRename = { showRenameDialogFor = session.id },
                    onDismiss = { showSessionInfoDialog = null }
                )
            }
        }

        if (showRenameDialogFor != null) {
            sessions.find { it.id == showRenameDialogFor }?.let { session ->
                RenameSessionDialog(
                    currentTitle = session.title,
                    onConfirm = { newTitle ->
                        viewModel.renameSession(session.id!!, newTitle)
                        showRenameDialogFor = null
                    },
                    onDismiss = { showRenameDialogFor = null }
                )
            }
        }

        if (detailItems != null) {
            SimpleDetailListDialog(
                data = detailItems!!,
                onDismiss = { viewModel.dismissDetailItems() }
            )
        }

        if (showSettingsSheet) {
            SettingsBottomSheet(
                selectedTheme = selectedTheme,
                onThemeSelected = { theme -> viewModel.setTheme(theme) },
                selectedLanguage = selectedLanguage,
                onLanguageSelected = { lang -> viewModel.setLanguage(lang) },
                onDismiss = { showSettingsSheet = false },
                sheetState = sheetState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentHeader(
    sessions: List<ChatSession>,
    currentSessionId: Int?,
    totalTokens: Int,
    totalCost: Double,
    messages: List<ChatMessage>,
    selectedModel: String,
    selectedHistoryLimit: Int,
    onMenuClick: () -> Unit,
    onModelSelected: (String) -> Unit,
    onHistoryLimitSelected: (Int) -> Unit,
    onDeleteSession: (Int) -> Unit
) {
    val colors = AgentTheme.colors
    var showModelSelector by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        title = {
            val sessionTitle = sessions.find { it.id == currentSessionId }?.title ?: "AI Agent Chat"
            Column {
                Text(
                    sessionTitle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                if (totalTokens > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "🪙 ${String.format(Locale.getDefault(), "%,d", totalTokens)}",
                            fontSize = 11.sp,
                            color = colors.metadataText
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "💸 ${Utils.formatCost(totalCost)}",
                            fontSize = 11.sp,
                            color = colors.metadataText
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "💬 ${messages.count { it.role == ChatMessage.ROLE_ASSISTANT }}",
                            fontSize = 11.sp,
                            color = colors.metadataText
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Outlined.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            Box {
                IconButton(onClick = { showModelSelector = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gemini_color),
                        contentDescription = "Select Model",
                        modifier = Modifier
                            .size(24.dp)
                            .alpha(if (selectedModel == AiModelCatalog.smartModelName || selectedModel == AiModelCatalog.liteModelName) 1f else 0.75f),
                        tint = if (selectedModel == AiModelCatalog.liteModelName)
                            colors.metadataText
                        else colors.geminiIconTint
                    )
                }
                DropdownMenu(
                    expanded = showModelSelector,
                    onDismissRequest = { showModelSelector = false },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = colors.surface,
                    modifier = Modifier.width(220.dp)
                ) {
                    Text(
                        text = stringResource(R.string.select_model),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    listOf(
                        AiModelCatalog.liteModelName to "Flash Lite (Fast)",
                        AiModelCatalog.defaultModelName to "Flash (Balanced)",
                        AiModelCatalog.smartModelName to "Pro (Smart)"
                    ).forEach { (modelId, label) ->
                        DropdownMenuItem(
                            text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onModelSelected(modelId)
                                showModelSelector = false
                            },
                            leadingIcon = {
                                RadioButton(
                                    selected = selectedModel == modelId,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
                        thickness = 0.5.dp,
                        color = colors.onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    Text(
                        text = stringResource(R.string.select_history_limit),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    listOf(
                        20 to stringResource(R.string.history_20),
                        50 to stringResource(R.string.history_50),
                        -1 to stringResource(R.string.history_all)
                    ).forEach { (limit, label) ->
                        DropdownMenuItem(
                            text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onHistoryLimitSelected(limit)
                                showModelSelector = false
                            },
                            leadingIcon = {
                                RadioButton(
                                    selected = selectedHistoryLimit == limit,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            IconButton(onClick = {
                currentSessionId?.let { onDeleteSession(it) }
            }) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete Current Chat")
            }
        }
    )
}

@Composable
fun ChatArea(
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    messages: List<ChatMessage>,
    currentSessionId: Int?,
    isLoading: Boolean,
    isStreaming: Boolean,
    listState: LazyListState,
    onInfoClick: (ChatMessage) -> Unit,
    onImageClick: (String) -> Unit,
    onAskAi: (String) -> Unit,
    onShowDetailsAtIndex: (ChatMessage, Int) -> Unit
) {
    val colors = AgentTheme.colors
    val streamingMessageId = messages.lastOrNull { it.role == ChatMessage.ROLE_ASSISTANT }?.id

    if (messages.isEmpty() && currentSessionId == null) {
        Box(
            modifier = modifier.fillMaxSize().padding(bottom = bottomContentPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gemini_color),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = colors.geminiIconTint
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.welcome_msg), style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
                Text(stringResource(R.string.welcome_desc), style = MaterialTheme.typography.bodyMedium, color = colors.metadataText)
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 16.dp + bottomContentPadding
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true
        ) {
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        AiLoadingIndicator(isClockwiseRotation = true, modifier = Modifier.size(32.dp))
                    }
                }
            }
            items(
                messages.asReversed(),
                key = { it.id ?: -it.timestamp.toInt() },
                contentType = { if (it.role == ChatMessage.ROLE_USER) "user" else "assistant" }
            ) { message ->
                if (message.content.isNotEmpty()) {
                    ChatBubble(
                        message,
                        isStreaming = isStreaming && message.id == streamingMessageId,
                        onInfoClick = { onInfoClick(message) },
                        onImageClick = onImageClick,
                        onAskAi = onAskAi,
                        onShowDetailsAtIndex = { index ->
                            onShowDetailsAtIndex(message, index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputArea(
    modifier: Modifier = Modifier,
    chatInput: String,
    isLoading: Boolean,
    onChatInputChanged: (String) -> Unit,
    onSendMessage: (String, List<Uri>, List<Uri>) -> Unit,
    onStopGeneration: () -> Unit,
    keyboardController: SoftwareKeyboardController?
) {
    val colors = AgentTheme.colors
    val context = LocalContext.current
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedFileUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(chatInput, TextRange(chatInput.length)))
    }

    LaunchedEffect(chatInput) {
        if (textFieldValue.text != chatInput) {
            textFieldValue = TextFieldValue(chatInput, TextRange(chatInput.length))
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedImageUris = (selectedImageUris + uris).distinct().take(10 - selectedFileUris.size)
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedFileUris = (selectedFileUris + uris).distinct().take(10 - selectedImageUris.size)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) pendingCameraUri?.let { selectedImageUris = (selectedImageUris + it).take(10 - selectedFileUris.size) }
        pendingCameraUri = null
    }

    val slashCommands = AgentViewModel.slashCommands
    val kotlinCommands = AgentViewModel.kotlinCommands
    val pythonCommands = AgentViewModel.pythonCommands
    val javaCommands = AgentViewModel.javaCommands
    val cCommands = AgentViewModel.cCommands
    val cppCommands = AgentViewModel.cppCommands
    val csharpCommands = AgentViewModel.csharpCommands
    val sqlCommands = AgentViewModel.sqlCommands
    val javascriptCommands = AgentViewModel.javascriptCommands
    val allSlashCommands = remember {
        (slashCommands + kotlinCommands + pythonCommands + javaCommands + cCommands + cppCommands +
            csharpCommands + sqlCommands + javascriptCommands + "/mermaid-error")
            .distinct()
    }
    val slashQuery = chatInput
        .takeIf { it.startsWith("/") && !it.contains(Regex("\\s")) }
        ?.lowercase()
    val matchingSlashCommands = slashQuery?.let { query ->
        if (query == "/") {
            slashCommands
        } else {
            allSlashCommands.filter { it.startsWith(query) }
        }
    }.orEmpty()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = colors.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (selectedImageUris.isNotEmpty() || selectedFileUris.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(start = 44.dp, top = 8.dp, bottom = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedImageUris.forEachIndexed { index, uri ->
                        Box {
                            AsyncImage(
                                model = uri,
                                contentDescription = "첨부 이미지 ${index + 1}",
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            IconButton(
                                onClick = {
                                    selectedImageUris = selectedImageUris.filterIndexed { itemIndex, _ -> itemIndex != index }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .background(colors.background.copy(alpha = 0.75f), CircleShape)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "첨부 이미지 제거", tint = colors.attachmentRemoveIcon)
                            }
                        }
                    }
                    selectedFileUris.forEachIndexed { index, uri ->
                        AssistChip(
                            onClick = { selectedFileUris = selectedFileUris.filterIndexed { itemIndex, _ -> itemIndex != index } },
                            label = { Text(uri.lastPathSegment?.substringAfterLast('/') ?: "파일") },
                            leadingIcon = { Icon(Icons.Outlined.Create, contentDescription = null) },
                            trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = "파일 제거") }
                        )
                    }
                }
            }

            if (!isLoading && matchingSlashCommands.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp, top = 4.dp, bottom = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    matchingSlashCommands.forEach { command ->
                        AssistChip(
                            onClick = {
                                textFieldValue = TextFieldValue(command, TextRange(command.length))
                                onChatInputChanged(command)
                            },
                            label = { Text(command) }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(
                        onClick = { showAttachmentMenu = true },
                        enabled = selectedImageUris.size + selectedFileUris.size < 10
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "첨부", tint = AgentTheme.colors.assistantText)
                    }
                    DropdownMenu(
                        expanded = showAttachmentMenu,
                        onDismissRequest = { showAttachmentMenu = false },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = colors.surface,
                        modifier = Modifier.width(180.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pick_photo), style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Outlined.Photo, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            onClick = {
                                showAttachmentMenu = false
                                galleryLauncher.launch("image/*")
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pick_file), style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            onClick = {
                                showAttachmentMenu = false
                                fileLauncher.launch("*/*")
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.take_photo), style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Outlined.CameraEnhance, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            onClick = {
                                showAttachmentMenu = false
                                createChatImageUri(context).let { uri ->
                                    pendingCameraUri = uri
                                    cameraLauncher.launch(uri)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        onChatInputChanged(it.text)
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            stringResource(R.string.input_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onBackground.copy(alpha = 0.6f)
                        )
                    },
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = colors.primary,
                        focusedTextColor = colors.assistantText,
                        unfocusedTextColor = colors.assistantText
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.assistantText)
                )

                IconButton(
                    onClick = {
                        if (isLoading) {
                            onStopGeneration()
                        } else if (chatInput.isNotBlank() || selectedImageUris.isNotEmpty() || selectedFileUris.isNotEmpty()) {
                            onSendMessage(chatInput, selectedImageUris, selectedFileUris)
                            selectedImageUris = emptyList()
                            selectedFileUris = emptyList()
                            keyboardController?.hide()
                        }
                    },
                    enabled = isLoading || chatInput.isNotBlank() || selectedImageUris.isNotEmpty() || selectedFileUris.isNotEmpty(),
                    modifier = Modifier
                        .padding(4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLoading) colors.errorContainer
                            else if (chatInput.isNotBlank() || selectedImageUris.isNotEmpty() || selectedFileUris.isNotEmpty()) colors.primary
                            else colors.surface.copy(alpha = 0.3f)
                        )
                ) {
                    Icon(
                        imageVector = if (isLoading) Icons.Default.Close else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isLoading) "Stop" else "Send",
                        tint = if (isLoading) colors.onErrorContainer
                        else if (chatInput.isNotBlank() || selectedImageUris.isNotEmpty() || selectedFileUris.isNotEmpty()) colors.onPrimary
                        else colors.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isStreaming: Boolean = false,
    onInfoClick: (ChatMessage) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onAskAi: (String) -> Unit = {},
    onShowDetailsAtIndex: (Int) -> Unit = {}
) {
    val isUser = message.role == ChatMessage.ROLE_USER
    val colors = AgentTheme.colors
    val bubbleColor = if (isUser) colors.userBubble else colors.assistantBubble

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = if (isUser) Modifier.wrapContentSize() else Modifier.fillMaxWidth(),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = bubbleColor,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 0.dp,
                modifier = if (isUser) Modifier.widthIn(max = 320.dp) else Modifier.fillMaxWidth()
            ) {
                AiChatMarkdownView(
                    markdown = message.content,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    isUser = isUser,
                    isStreaming = isStreaming && !isUser,
                    onImageClick = onImageClick,
                    onAskAi = onAskAi,
                    onShowDetailsAtIndex = onShowDetailsAtIndex
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            ) {
                if (isUser) {
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 10.sp,
                        color = colors.onBackground.copy(alpha = 0.5f)
                    )
                }
                if (!isUser && message.role == ChatMessage.ROLE_ASSISTANT) {
                    AiMetadataView(message, onInfoClick = { onInfoClick(message) })
                }
            }
        }
    }
}

@Composable
fun AiMetadataView(
    message: ChatMessage,
    onInfoClick: () -> Unit
) {
    val colors = AgentTheme.colors
    Row(
        modifier = Modifier.padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onInfoClick, modifier = Modifier.size(16.dp).offset(y = 0.725.dp)) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Details",
                tint = colors.metadataText.copy(alpha = 0.6f),
                modifier = Modifier.size(13.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        val infoText = buildString {
            append("🤖 ${message.modelName ?: stringResource(R.string.unknown)}  ")
            append("🪙 ${String.format(Locale.getDefault(), "%,d", message.totalTokens ?: 0)}  ")
            append("💸 ${Utils.formatCost(message.estimatedCostUsd ?: 0.0)}  ")
            append("⏱️ ${Utils.formatDurationMs(message.responseTimeMs ?: 0)}")
        }
        Text(text = infoText, fontSize = 10.sp, color = colors.metadataText.copy(alpha = 0.8f))
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("a h:mm", Locale.KOREA)
    return sdf.format(Date(timestamp))
}
