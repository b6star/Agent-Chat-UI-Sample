package com.b6star.chatui.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.b6star.chatui.R
import com.b6star.chatui.ai.ChatResponse
import com.b6star.chatui.data.model.ChatMessage
import com.b6star.chatui.data.model.ChatSession
import com.b6star.chatui.ui.theme.AgentTheme
import com.b6star.chatui.util.Utils
import com.b6star.chatui.viewmodel.AgentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SimpleDetailListDialog(
    data: ChatResponse.ShowDetails,
    onDismiss: () -> Unit
) {
    val colors = AgentTheme.colors
    val sortedItems = remember(data.items) { data.items.reversed() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(data.title, fontWeight = FontWeight.Bold, color = colors.onBackground) },
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                shape = RoundedCornerShape(12.dp),
                color = colors.surface.copy(alpha = 0.5f)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedItems) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(item.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.date, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                Text(
                                    Utils.formatCurrency(item.amount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.primary
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun MessageDetailDialog(
    message: ChatMessage,
    onDismiss: () -> Unit
) {
    val colors = AgentTheme.colors
    var isFirstPromptExpanded by remember { mutableStateOf(false) }
    var isFirstResponseExpanded by remember { mutableStateOf(false) }
    var isSecondPromptExpanded by remember { mutableStateOf(false) }
    var isSecondResponseExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.details),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DetailRow(stringResource(R.string.provider_label), message.provider ?: "mock")
                DetailRow(stringResource(R.string.model_label), message.modelName ?: stringResource(R.string.unknown))
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                DetailRow("Response Time", Utils.formatDurationMs(message.responseTimeMs ?: 0))
                DetailRow("Prompt Tokens", String.format(Locale.getDefault(), "%,d", message.promptTokens ?: 0))
                DetailRow("Candidates Tokens", String.format(Locale.getDefault(), "%,d", message.candidatesTokens ?: 0))
                if ((message.thoughtsTokens ?: 0) > 0) {
                    DetailRow("Thoughts Tokens", String.format(Locale.getDefault(), "%,d", message.thoughtsTokens ?: 0))
                }
                DetailRow("Total Tokens", String.format(Locale.getDefault(), "%,d", message.totalTokens ?: 0))
                DetailRow("Estimated Cost", Utils.formatCost(message.estimatedCostUsd ?: 0.0))

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                DetailRow("Device", message.deviceModel ?: Utils.getDeviceModel())
                DetailRow("OS", message.osVersion ?: Utils.getOsVersion())

                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date(message.timestamp))
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(message.timestamp))
                DetailRow("Date", dateStr)
                DetailRow("Time", timeStr)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                DetailRow("Input History Count", (message.inputHistoryCount ?: 0).toString())

                if (message.firstPassPrompt != null || message.firstPassResponse != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    message.firstPassPrompt?.let {
                        ExpandablePassSection(
                            title = stringResource(R.string.pass_1_prompt),
                            content = it,
                            isExpanded = isFirstPromptExpanded,
                            onToggle = { isFirstPromptExpanded = !isFirstPromptExpanded },
                            charCount = it.length
                        )
                    }

                    message.firstPassResponse?.let {
                        ExpandablePassSection(
                            title = stringResource(R.string.pass_1_json),
                            content = it,
                            isExpanded = isFirstResponseExpanded,
                            onToggle = { isFirstResponseExpanded = !isFirstResponseExpanded },
                            charCount = it.length
                        )
                    }
                }

                if (message.secondPassPrompt != null || message.secondPassResponse != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    message.secondPassPrompt?.let {
                        ExpandablePassSection(
                            title = stringResource(R.string.pass_2_prompt),
                            content = it,
                            isExpanded = isSecondPromptExpanded,
                            onToggle = { isSecondPromptExpanded = !isSecondPromptExpanded },
                            charCount = it.length
                        )
                    }

                    message.secondPassResponse?.let {
                        ExpandablePassSection(
                            title = stringResource(R.string.pass_2_json),
                            content = it,
                            isExpanded = isSecondResponseExpanded,
                            onToggle = { isSecondResponseExpanded = !isSecondResponseExpanded },
                            charCount = it.length
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
fun ExpandablePassSection(
    title: String,
    content: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    charCount: Int
) {
    val colors = AgentTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$title (${charCount}자)",
                fontSize = 12.sp,
                color = colors.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = colors.background
            ) {
                SelectionContainer {
                    Text(
                        text = content,
                        color = colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SessionInfoDialog(
    session: ChatSession,
    stats: AgentViewModel.SessionStats?,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AgentTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.session_info),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DetailRow(stringResource(R.string.session_title_label), session.title)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                DetailRow(
                    "Remote Sync",
                    if (session.remoteId.isBlank()) "Local only" else "Synced"
                )
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date(session.lastMessageTime))
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(session.lastMessageTime))
                DetailRow("Last Updated", "$dateStr $timeStr")
                DetailRow("Messages", "${stats?.assistantMessageCount ?: 0}")
                DetailRow(
                    "Avg. Response Time",
                    stats?.avgResponseTimeMs?.let { Utils.formatDurationMs(it) } ?: "-"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                DetailRow("Total Tokens", String.format(Locale.getDefault(), "%,d", stats?.totalTokens ?: 0))
                DetailRow("Total Est. Cost", Utils.formatCost(stats?.totalCost ?: 0.0))

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val buttonColors = ButtonDefaults.buttonColors(
                        containerColor = colors.onBackground,
                        contentColor = colors.background
                    )
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = buttonColors
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                    Button(
                        onClick = onRename,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = buttonColors
                    ) {
                        Text(stringResource(R.string.rename))
                    }
                }
            }
        }
    }
}

@Composable
fun RenameSessionDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AgentTheme.colors
    var text by remember { mutableStateOf(currentTitle) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
        keyboard?.show()
    }

    AlertDialog(
        onDismissRequest = {
            keyboard?.hide()
            onDismiss()
        },
        title = { Text(stringResource(R.string.rename_title), fontWeight = FontWeight.Bold, color = colors.onBackground) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                label = { Text(stringResource(R.string.new_name_label)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.onBackground,
                    unfocusedTextColor = colors.onBackground,
                    cursorColor = colors.primary,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.onSurfaceVariant
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    keyboard?.hide()
                    onConfirm(text)
                },
                enabled = text.trim().isNotEmpty()
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                keyboard?.hide()
                onDismiss()
            }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    val colors = AgentTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.metadataText
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colors.onBackground,
            textAlign = TextAlign.End
        )
    }
}
