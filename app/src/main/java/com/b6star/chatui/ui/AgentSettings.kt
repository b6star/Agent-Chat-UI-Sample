package com.b6star.chatui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.b6star.chatui.R
import com.b6star.chatui.ui.theme.AgentTheme
import com.b6star.chatui.ui.theme.AgentThemeType
import com.b6star.chatui.viewmodel.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    selectedTheme: AgentThemeType,
    onThemeSelected: (AgentThemeType) -> Unit,
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    val colors = AgentTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Theme settings section
            Text(stringResource(R.string.theme_selection), style = MaterialTheme.typography.labelLarge, color = colors.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeOption(
                    label = "Default",
                    color = Color(0xFF1A1A1B),
                    isSelected = selectedTheme == AgentThemeType.DEFAULT,
                    onClick = { onThemeSelected(AgentThemeType.DEFAULT) }
                )
                ThemeOption(
                    label = "Blue",
                    color = Color(0xFF1565C0),
                    isSelected = selectedTheme == AgentThemeType.BLUE,
                    onClick = { onThemeSelected(AgentThemeType.BLUE) }
                )
                ThemeOption(
                    label = "Green",
                    color = Color(0xFF2E7D32),
                    isSelected = selectedTheme == AgentThemeType.GREEN,
                    onClick = { onThemeSelected(AgentThemeType.GREEN) }
                )
                ThemeOption(
                    label = "Red",
                    color = Color(0xFFDC2626),
                    isSelected = selectedTheme == AgentThemeType.RED,
                    onClick = { onThemeSelected(AgentThemeType.RED) }
                )
                ThemeOption(
                    label = "Purple",
                    color = Color(0xFF9333EA),
                    isSelected = selectedTheme == AgentThemeType.PURPLE,
                    onClick = { onThemeSelected(AgentThemeType.PURPLE) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Language settings section
            Text(stringResource(R.string.language_settings), style = MaterialTheme.typography.labelLarge, color = colors.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppLanguage.entries.forEach { language ->
                    val displayName = when(language) {
                        AppLanguage.SYSTEM -> stringResource(R.string.lang_system)
                        AppLanguage.ENGLISH -> stringResource(R.string.lang_en)
                        AppLanguage.KOREAN -> stringResource(R.string.lang_ko)
                    }
                    LanguageOption(
                        label = displayName,
                        isSelected = selectedLanguage == language,
                        onClick = { onLanguageSelected(language) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // General settings section
            Text(stringResource(R.string.general_section), style = MaterialTheme.typography.labelLarge, color = colors.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingsItem(
                icon = Icons.Outlined.Person,
                title = stringResource(R.string.user_profile),
                subtitle = stringResource(R.string.user_profile_desc)
            )
            SettingsItem(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.notifications),
                subtitle = stringResource(R.string.notifications_desc)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // AI settings section
            Text(stringResource(R.string.ai_preferences), style = MaterialTheme.typography.labelLarge, color = colors.primary)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Outlined.Key,
                title = stringResource(R.string.api_key_mgmt),
                subtitle = stringResource(R.string.api_key_desc)
            )
            
            var systemPromptEnabled by remember { mutableStateOf(true) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Psychology, contentDescription = null, tint = colors.onSurfaceVariant)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.custom_prompt), style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                    Text(stringResource(R.string.custom_prompt_desc), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Switch(checked = systemPromptEnabled, onCheckedChange = { systemPromptEnabled = it })
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Others
            Text(stringResource(R.string.others_section), style = MaterialTheme.typography.labelLarge, color = colors.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingsItem(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.app_info),
                subtitle = stringResource(R.string.app_version)
            )
        }
    }
}

@Composable
fun ThemeOption(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = AgentTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(70.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .padding(4.dp)
        ) {
            if (isSelected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) colors.primary else colors.onSurfaceVariant
        )
    }
}

@Composable
fun LanguageOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = AgentTheme.colors
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (isSelected) {
            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.primary.copy(alpha = 0.12f),
            selectedLabelColor = colors.primary,
            selectedLeadingIconColor = colors.primary,
            labelColor = colors.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = colors.onSurfaceVariant.copy(alpha = 0.2f),
            selectedBorderColor = colors.primary.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val colors = AgentTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Click event */ }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}
