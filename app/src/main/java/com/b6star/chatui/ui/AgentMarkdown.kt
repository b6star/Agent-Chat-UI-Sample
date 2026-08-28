package com.b6star.chatui.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    onSlashCommand: (String) -> Unit = {},
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
                                        if (it.item.startsWith("SLASH_COMMAND:")) {
                                            onSlashCommand(it.item.removePrefix("SLASH_COMMAND:"))
                                        } else if (it.item.startsWith("SHOW_DETAILS:")) {
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
                            declaredLanguage = block.language,
                            mermaid = isMermaid && !isStreaming,
                            renderKey = if (isStreaming) 0 else markdown.hashCode(),
                            onAskAi = onAskAi
                        )
                    }
                    MarkdownBlock.Spacer -> Spacer(modifier = Modifier.height(12.dp))
                    MarkdownBlock.Divider -> {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                            thickness = 1.dp,
                            color = colors.onBackground.copy(alpha = if (colors.isDark) 0.2f else 0.15f)
                        )
                    }
                    is MarkdownBlock.Table -> {
                        MarkdownTableView(block, colors)
                    }
                    is MarkdownBlock.Image -> {
                        AsyncImage(
                            model = block.url,
                            contentDescription = block.alt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClick(block.url) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownTableView(table: MarkdownBlock.Table, colors: AgentColors) {
    val scrollState = rememberScrollState()
    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .padding(vertical = 8.dp),
        color = colors.background.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.onBackground.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            // Headers
            Row(modifier = Modifier.background(colors.onBackground.copy(alpha = 0.05f))) {
                table.headers.forEach { header ->
                    TableCell(text = header, isHeader = true, colors = colors)
                }
            }
            // Rows
            table.rows.forEach { row ->
                HorizontalDivider(color = colors.onBackground.copy(alpha = 0.05f))
                Row {
                    row.forEach { cell ->
                        TableCell(text = cell, isHeader = false, colors = colors)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(text: String, isHeader: Boolean, colors: AgentColors) {
    val aiPrefix = stringResource(com.b6star.chatui.R.string.ai_answer_prefix)
    val showDetails = stringResource(com.b6star.chatui.R.string.show_details_link)
    val annotatedText = remember(text, colors) {
        markdownText(text, colors, aiPrefix, showDetails)
    }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .widthIn(min = 80.dp, max = 240.dp)
    ) {
        ClickableText(
            text = annotatedText,
            style = (if (isHeader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall).copy(
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                color = if (isHeader) colors.primary else colors.onBackground
            ),
            onClick = { offset ->
                annotatedText.getStringAnnotations("URL", offset, offset)
                    .firstOrNull()?.item?.let { url ->
                        openInBrowser(context, url)
                    }
            }
        )
    }
}

sealed interface MarkdownBlock {
    data class Text(val value: String) : MarkdownBlock
    data class Code(val value: String, val language: String) : MarkdownBlock
    data object Spacer : MarkdownBlock
    data object Divider : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data class Image(val url: String, val alt: String) : MarkdownBlock
}

private const val THINKING_STEP_PREFIX = "THINKING_STEP"
private val THINKING_STEP_SYMBOLS = setOf("🔍", "📊", "⚙️", "📝", "🔄", "✅", "📅", "🏷️")

fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val result = mutableListOf<MarkdownBlock>()
    val textBuffer = StringBuilder()
    var codeBuffer: StringBuilder? = null
    var language = ""
    
    // Table parsing state
    val currentTableRows = mutableListOf<List<String>>()

    fun flushText() {
        val raw = textBuffer.toString()
        if (raw.isBlank()) {
            if (raw.count { it == '\n' } >= 2) {
                if (result.lastOrNull() !is MarkdownBlock.Spacer) {
                    result += MarkdownBlock.Spacer
                }
            }
        } else {
            if (raw.startsWith("\n\n") && result.lastOrNull() !is MarkdownBlock.Spacer) {
                result += MarkdownBlock.Spacer
            }
            result += MarkdownBlock.Text(raw.trim())
            if (raw.endsWith("\n\n")) {
                result += MarkdownBlock.Spacer
            }
        }
        textBuffer.clear()
    }

    fun flushTable() {
        if (currentTableRows.isNotEmpty()) {
            val headers = currentTableRows.first()
            val rows = currentTableRows.drop(if (currentTableRows.size > 1 && currentTableRows[1].all { it.contains("-") || it.contains(":") }) 2 else 1)
            result += MarkdownBlock.Table(headers, rows)
            currentTableRows.clear()
        }
    }

    markdown.lines().forEach { line ->
        val trimmed = line.trim()
        
        // Image detection: ![alt](url)
        val imageMatch = Regex("""^!\[(.*?)\]\((.*?)\)$""").find(trimmed)
        
        when {
            trimmed.startsWith("```") -> {
                flushTable()
                if (codeBuffer == null) {
                    flushText()
                    codeBuffer = StringBuilder()
                    language = trimmed.removePrefix("```").trim().lowercase()
                } else {
                    result += MarkdownBlock.Code(codeBuffer.toString().trimEnd(), language)
                    codeBuffer = null
                    language = ""
                }
            }
            codeBuffer != null -> {
                codeBuffer.appendLine(line)
            }
            (trimmed == "---" || trimmed == "___" || trimmed == "***") -> {
                flushTable()
                flushText()
                result += MarkdownBlock.Divider
            }
            imageMatch != null -> {
                flushTable()
                flushText()
                result += MarkdownBlock.Image(url = imageMatch.groupValues[2], alt = imageMatch.groupValues[1])
            }
            trimmed.startsWith("|") && trimmed.endsWith("|") -> {
                flushText()
                val cells = trimmed.split("|").filterIndexed { i, _ -> i != 0 && i != trimmed.split("|").lastIndex }.map { it.trim() }
                currentTableRows.add(cells)
            }
            else -> {
                if (currentTableRows.isNotEmpty()) {
                    flushTable()
                }
                textBuffer.appendLine(line)
            }
        }
    }

    flushTable()
    if (codeBuffer != null) {
        result += MarkdownBlock.Code(codeBuffer.toString().trimEnd(), language)
    } else {
        flushText()
    }

    while (result.firstOrNull() is MarkdownBlock.Spacer) result.removeAt(0)
    while (result.lastOrNull() is MarkdownBlock.Spacer) result.removeAt(result.lastIndex)

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
        var currentLine = line
        val visualLine = currentLine.replace(Regex("^\\t+")) { "    ".repeat(it.value.length) }
        val clean = currentLine.trimStart()

        // 1. Blockquote handling (Nested support)
        val quoteLevel = clean.takeWhile { it == '>' || it == ' ' }.count { it == '>' }
        if (quoteLevel > 0) {
            val quoteColor = colors.quote.copy(alpha = 0.6f)
            repeat(quoteLevel) {
                withStyle(SpanStyle(color = quoteColor, fontWeight = FontWeight.Bold)) {
                    append("▎")
                }
                append(" ")
            }
            // Remove the quote prefix for further processing
            currentLine = clean.dropWhile { it == '>' || it == ' ' }
        }

        val processingLine = currentLine.trimStart()
        val heading = processingLine.takeWhile { it == '#' }.length
        val headingContent = processingLine.removePrefix("#".repeat(heading)).trim()

        val thinkingStepSymbol = THINKING_STEP_SYMBOLS.firstOrNull { symbol ->
            processingLine.startsWith(THINKING_STEP_PREFIX + symbol)
        }
        val isThinkingStep = thinkingStepSymbol != null

        when {
            isThinkingStep -> {
                withStyle(SpanStyle(
                    color = colors.metadataText.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic
                )) {
                    appendInlineMarkdown(
                        value = processingLine.replaceFirst(THINKING_STEP_PREFIX, ""),
                        colors = colors
                    )
                }
            }
            processingLine.startsWith(aiPrefix) -> {
                withStyle(SpanStyle(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = colors.primary
                )) {
                    appendInlineMarkdown(value = processingLine, colors = colors)
                }
            }
            processingLine.contains(showDetails) -> {
                val linkText = " $showDetails"
                append(processingLine.replace(showDetails, ""))
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
            }
            heading > 0 -> {
                val fontSize = (48 - heading * 10).let { if (it < 14) 14 else it }.sp
                val isListInHeader = headingContent.startsWith("-") || headingContent.startsWith("*")
                val finalContent = if (isListInHeader) headingContent.drop(1).trimStart() else headingContent
                val headerSymbol = if (isListInHeader) "• " else ""

                withStyle(SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    color = colors.primary
                )) {
                    if (headerSymbol.isNotEmpty()) append(headerSymbol)
                    appendInlineMarkdown(value = finalContent, colors = colors)
                }
            }
            processingLine.startsWith("- ") || processingLine.startsWith("* ") -> {
                val (symbol, contentAfterSymbol) = when {
                    processingLine.startsWith("- [x] ") || processingLine.startsWith("* [x] ") -> "☑ " to processingLine.drop(6)
                    processingLine.startsWith("- [ ] ") || processingLine.startsWith("* [ ] ") -> "☐ " to processingLine.drop(6)
                    else -> "• " to processingLine.drop(2)
                }

                val symbolColor = if (colors.isDark) Color.White else Color.Black
                withStyle(SpanStyle(color = symbolColor)) {
                    append(symbol)
                }
                appendInlineMarkdown(value = contentAfterSymbol, colors = colors)
            }
            processingLine.matches(Regex("^\\d+\\.\\s.*")) -> {
                val number = processingLine.substringBefore(".")
                withStyle(SpanStyle(color = colors.onBackground)) {
                    append("$number. ")
                }
                appendInlineMarkdown(value = processingLine.substringAfter(". ").trim(), colors = colors)
            }
            else -> {
                appendInlineMarkdown(value = if (quoteLevel > 0) processingLine else visualLine, colors = colors)
            }
        }
        
        if (index < lines.lastIndex) append("\n")
    }
}

fun AnnotatedString.Builder.appendInlineMarkdown(
    value: String,
    colors: AgentColors
) {
    // Escaping support for common symbols: \* \_ \` \# \[ \] \~
    val escapeMap = mapOf(
        "\\*" to "*", "\\_" to "_", "\\`" to "`", "\\#" to "#",
        "\\[" to "[", "\\]" to "]", "\\~" to "~"
    )
    var processedValue = value
    escapeMap.forEach { (escaped, raw) ->
        processedValue = processedValue.replace(escaped, "\uE000${raw}\uE001")
    }

    val regex = Regex("""(\*\*\*.+?\*\*\*)|(\*\*.+?\*\*)|(\*.+?\*)|(~~.+?~~)|(`.+?`)|(\[.+?\]\(.+?\))|(https?://[^\s()]+)""")
    var cursor = 0

    regex.findAll(processedValue).forEach { match ->
        append(restoreEscaped(processedValue.substring(cursor, match.range.first)))
        val token = match.value

        when {
            token.startsWith("***") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = colors.emphasis)) {
                    append(restoreEscaped(token.drop(3).dropLast(3)))
                }
            }
            token.startsWith("**") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.emphasis)) {
                    append(restoreEscaped(token.drop(2).dropLast(2)))
                }
            }
            token.startsWith("*") -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(restoreEscaped(token.drop(1).dropLast(1)))
                }
            }
            token.startsWith("~~") -> {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(restoreEscaped(token.drop(2).dropLast(2)))
                }
            }
            token.startsWith("`") -> {
                val inlineCode = restoreEscaped(token.drop(1).dropLast(1))
                if (inlineCode.startsWith("/")) {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = colors.primary,
                        textDecoration = TextDecoration.Underline
                    )) {
                        val start = length
                        append(inlineCode)
                        addStringAnnotation("ACTION", "SLASH_COMMAND:$inlineCode", start, length)
                    }
                    cursor = match.range.last + 1
                    return@forEach
                }
                withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = colors.inlineCodeBackground,
                    color = colors.inlineCodeText
                )) {
                    append(inlineCode)
                }
            }
            token.startsWith("[") -> {
                val linkText = restoreEscaped(token.substringAfter("[").substringBefore("]"))
                val linkUrl = token.substringAfter("](").removeSuffix(")")
                withStyle(SpanStyle(
                    color = colors.primary,
                    textDecoration = TextDecoration.Underline
                )) {
                    addStringAnnotation("URL", linkUrl, start = length, end = length + linkText.length)
                    append(linkText)
                }
            }
            token.startsWith("http") -> {
                withStyle(SpanStyle(
                    color = colors.primary,
                    textDecoration = TextDecoration.Underline
                )) {
                    addStringAnnotation("URL", token, start = length, end = length + token.length)
                    append(token)
                }
            }
        }

        cursor = match.range.last + 1
    }

    append(restoreEscaped(processedValue.substring(cursor)))
}

private fun restoreEscaped(text: String): String {
    return text.replace("\uE000", "").replace("\uE001", "")
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
