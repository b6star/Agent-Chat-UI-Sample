package com.b6star.chatui.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.b6star.chatui.R
import com.b6star.chatui.data.model.ChatSession
import com.b6star.chatui.ui.theme.AgentTheme

@Composable
fun AgentPanel(
    drawerState: DrawerState,
    sessions: List<ChatSession>,
    currentSessionId: Int?,
    onSessionClick: (Int) -> Unit,
    onSessionInfoClick: (Int) -> Unit,
    onCreateNewChat: () -> Unit,
    onSettingsClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = AgentTheme.colors

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxHeight()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.recent_chats),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onBackground
                        )

                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(sessions) { session ->
                                NavigationDrawerItem(
                                    label = {
                                        Text(
                                            session.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    selected = session.id == currentSessionId,
                                    onClick = { onSessionClick(session.id!!) },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = colors.primary.copy(alpha = 0.12f),
                                        selectedIconColor = colors.primary,
                                        selectedTextColor = colors.primary,
                                        unselectedContainerColor = Color.Transparent,
                                        unselectedIconColor = colors.onSurfaceVariant,
                                        unselectedTextColor = colors.onBackground
                                    ),
                                    badge = {
                                        IconButton(
                                            onClick = { onSessionInfoClick(session.id!!) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Info,
                                                contentDescription = stringResource(R.string.info),
                                                modifier = Modifier.size(20.dp),
                                                tint = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onCreateNewChat,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.new_chat))
                            }

                            FilledTonalIconButton(
                                onClick = onSettingsClick,
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        },
        content = content
    )
}
