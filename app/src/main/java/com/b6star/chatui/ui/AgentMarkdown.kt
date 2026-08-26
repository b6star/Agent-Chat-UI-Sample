package com.b6star.chatui.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b6star.chatui.ui.theme.AgentColors
import com.b6star.chatui.ui.theme.AgentTheme

@Composable
fun AiChatMarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    isUser: Boolean = false,
    isStreaming: Boolean = false,
    onImageClick: (String) -> Unit = {},
    onAskAi: (String) -> Unit = {},
    onShowDetailsAtIndex: (Int) -> Unit = {}
) {
    val colors = AgentTheme.colors
    val context = LocalContext.current
    val textColor = if (isUser) colors.userText else colors.assistantText

    SelectionContainer {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Text -> {
                        val aiPrefix = stringResource(com.b6star.chatui.R.string.ai_answer_prefix)
                        val showDetails = stringResource(com.b6star.chatui.R.string.show_details_link)
                        val text = remember(block.value, colors, aiPrefix, showDetails) {
                            markdownText(
                                value = block.value,
                                colors = colors,
                                aiPrefix = aiPrefix,
                                showDetails = showDetails
                            )
                        }
                        ClickableText(
                            text = text,
                            modifier = Modifier.padding(vertical = 2.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                            onClick = { offset ->
                                text.getStringAnnotations("URL", offset, offset)
                                    .firstOrNull()?.item?.let { url ->
                                        if (isImageUrl(url, context)) onImageClick(url)
                                        else openInBrowser(context, url)
                                        return@ClickableText
                                    }

                                text.getStringAnnotations("ACTION", offset, offset)
                                    .firstOrNull()?.let {
                                        if (it.item.startsWith("SHOW_DETAILS:")) {
                                            val index = it.item.substringAfter("SHOW_DETAILS:").toIntOrNull() ?: 0
                                            onShowDetailsAtIndex(index)
                                        }
                                    }
                            }
                        )
                    }
                    is MarkdownBlock.Code -> {
                        val isMermaid = block.language == "mermaid"
                        CodeWebView(
                            code = block.value,
                            mermaid = isMermaid && !isStreaming,
                            renderKey = if (isStreaming) 0 else markdown.hashCode(),
                            onAskAi = onAskAi
                        )
                    }
                    MarkdownBlock.Spacer -> Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

sealed interface MarkdownBlock {
    data class Text(val value: String) : MarkdownBlock
    data class Code(val value: String, val language: String) : MarkdownBlock
    data object Spacer : MarkdownBlock
}

fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val result = mutableListOf<MarkdownBlock>()
    val text = StringBuilder()
    var code: StringBuilder? = null
    var language = ""

    markdown.lines().forEach { line ->
        if (line.trimStart().startsWith("```")) {
            if (code == null) {
                if (text.isNotBlank()) {
                    val raw = text.toString()
                    result += MarkdownBlock.Text(raw.trim())
                    if (raw.endsWith("\n\n")) {
                        result += MarkdownBlock.Spacer
                    }
                } else if (text.isNotEmpty() && result.isNotEmpty() && result.last() != MarkdownBlock.Spacer) {
                    result += MarkdownBlock.Spacer
                }
                text.clear()
                code = StringBuilder()
                language = line.trim().removePrefix("```").trim().lowercase()
            } else {
                result += MarkdownBlock.Code(code.toString().trimEnd(), language)
                code = null
                language = ""
            }
        } else {
            if (code != null) {
                code.appendLine(line)
            } else {
                text.appendLine(line)
            }
        }
    }

    if (code != null) {
        result += MarkdownBlock.Code(code.toString().trimEnd(), language)
    } else if (text.isNotBlank()) {
        result += MarkdownBlock.Text(text.toString().trim())
    }

    while (result.firstOrNull() == MarkdownBlock.Spacer) result.removeAt(0)
    while (result.lastOrNull() == MarkdownBlock.Spacer) result.removeAt(result.lastIndex)

    return result
}

fun markdownText(
    value: String,
    colors: AgentColors,
    aiPrefix: String,
    showDetails: String
): AnnotatedString = buildAnnotatedString {
    var detailIndex = 0
    val lines = value.lines()

    lines.forEachIndexed { index, line ->
        val clean = line.trimStart()
        val headingIndent = line.takeWhile { it == '\t' || it == ' ' }
        val visualLine = line.replace(Regex("^\\t+")) { "    ".repeat(it.value.length) }
        val heading = clean.takeWhile { it == '#' }.length
        val content = clean.removePrefix("#".repeat(heading)).trim()

        val isThinkingStep = clean.startsWith("🔍") || clean.startsWith("📊") ||
                clean.startsWith("⚙️") || clean.startsWith("📝") ||
                clean.startsWith("🔄") || clean.startsWith("✅") ||
                clean.startsWith("📅") || clean.startsWith("🏷️")

        when {
            isThinkingStep -> {
                withStyle(SpanStyle(
                    color = colors.metadataText.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic
                )) {
                    appendInlineMarkdown(value = visualLine, colors = colors)
                    if (index < lines.lastIndex) append("\n")
                }
            }
            clean.startsWith(aiPrefix) -> {
                withStyle(SpanStyle(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = colors.primary
                )) {
                    appendInlineMarkdown(value = visualLine, colors = colors)
                    if (index < lines.lastIndex) append("\n")
                }
            }
            clean.contains(showDetails) -> {
                val linkText = " $showDetails"
                append(line.replace(showDetails, ""))
                val start = length
                withStyle(SpanStyle(
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )) {
                    append(linkText)
                }
                addStringAnnotation("ACTION", "SHOW_DETAILS:$detailIndex", start = start, end = length)
                detailIndex++
                if (index < lines.lastIndex) append("\n")
            }
            heading > 0 -> {
                val fontSize = (48 - heading * 10).let { if (it < 14) 14 else it }.sp
                val headingIndentWidth = (
                    headingIndent.count { it == '\t' } * 16 +
                        headingIndent.count { it == ' ' } * 4
                    ).sp

                val isListInHeader = content.startsWith("-") || content.startsWith("*")
                val finalContent = if (isListInHeader) content.drop(1).trimStart() else content
                val headerSymbol = if (isListInHeader) {
                    when (heading) {
                        1 -> "┃ "
                        2 -> "┋ "
                        3 -> "· "
                        else -> "- "
                    }
                } else ""
                val symbolColor = if (colors.isDark) Color.White else Color.Black

                withStyle(
                    androidx.compose.ui.text.TextStyle(
                        lineHeight = fontSize,
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                            alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                            trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
                        )
                    ).toParagraphStyle().copy(
                        textIndent = TextIndent(
                            firstLine = headingIndentWidth,
                            restLine = headingIndentWidth
                        )
                    )
                ) {
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        color = colors.primary
                    )) {
                        if (headerSymbol.isNotEmpty()) {
                            withStyle(SpanStyle(color = symbolColor)) {
                                append(headerSymbol)
                            }
                        }
                        appendInlineMarkdown(value = finalContent, colors = colors)
                    }
                // Move newline inside the block to follow the header\'s lineHeight
                if (index < lines.lastIndex) append("\n")
                }
            }
            clean.startsWith("> ") -> {
                withStyle(SpanStyle(
                    fontStyle = FontStyle.Italic,
                    color = colors.quote
                )) {
                    append("▎ ")
                    appendInlineMarkdown(value = clean.drop(2), colors = colors)
                    if (index < lines.lastIndex) append("\n")
                }
            }
            clean.startsWith("- ") || clean.startsWith("* ") -> {
                val indent = headingIndent.replace("\t", "    ")
                append(indent)
                val symbolColor = if (colors.isDark) Color.White else Color.Black
                withStyle(SpanStyle(color = symbolColor)) {
                    append("· ")
                }
                appendInlineMarkdown(value = clean.drop(2), colors = colors)
                if (index < lines.lastIndex) append("\n")
            }
            clean.matches(Regex("^\\d+\\.\\s.*")) -> {
                val number = clean.substringBefore(".")
                withStyle(SpanStyle(color = colors.onBackground)) {
                    append("$number. ")
                }
                appendInlineMarkdown(value = clean.substringAfter(". ").trim(), colors = colors)
                if (index < lines.lastIndex) append("\n")
            }
            else -> {
                appendInlineMarkdown(value = visualLine, colors = colors)
                if (index < lines.lastIndex) append("\n")
            }
        }
    }
}

fun AnnotatedString.Builder.appendInlineMarkdown(
    value: String,
    colors: AgentColors
) {
    val regex = Regex("""(\*\*.+?\*\*)|(\*.+?\*)|(`.+?`)|(\[.+?\]\(.+?\))""")
    var cursor = 0

    regex.findAll(value).forEach { match ->
        append(value.substring(cursor, match.range.first))
        val token = match.value

        when {
            token.startsWith("**") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.emphasis)) {
                    append(token.drop(2).dropLast(2))
                }
            }
            token.startsWith("*") -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(token.drop(1).dropLast(1))
                }
            }
            token.startsWith("`") -> {
                withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = colors.inlineCodeBackground,
                    color = colors.inlineCodeText
                )) {
                    append(token.drop(1).dropLast(1))
                }
            }
            token.startsWith("[") -> {
                val linkText = token.substringAfter("[").substringBefore("]")
                val linkUrl = token.substringAfter("](").removeSuffix(")")
                withStyle(SpanStyle(
                    color = colors.primary,
                    textDecoration = TextDecoration.Underline
                )) {
                    addStringAnnotation("URL", linkUrl, start = length, end = length + linkText.length)
                    append(linkText)
                }
            }
        }

        cursor = match.range.last + 1
    }

    append(value.substring(cursor))
}

fun isImageUrl(url: String, context: Context? = null): Boolean {
    if (url.startsWith("content://")) {
        val mimeType = context?.contentResolver?.getType(Uri.parse(url))
        if (mimeType != null) {
            return mimeType.startsWith("image/")
        }
        // Fallback to extension check if mime type is null
    }
    return Regex("\\.(png|jpe?g|gif|webp|heic)(\\?|%|$)", RegexOption.IGNORE_CASE).containsMatchIn(url)
}

fun openInBrowser(context: Context, url: String) {
    val target = if (!url.contains("://")) "https://$url" else url
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
    }
}
