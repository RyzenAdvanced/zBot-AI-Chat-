package com.example.ui.chat

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Message
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, onNavigateToSettings: () -> Unit) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val threads by viewModel.threads.collectAsStateWithLifecycle()
    val activeThreadId by viewModel.activeThreadId.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()
    val searchLogs by viewModel.searchProgressLogs.collectAsStateWithLifecycle()
    val activeArtifact by viewModel.activeArtifact.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Citation dialogue modal
    var selectedCitationSource by remember { mutableStateOf<CitationSource?>(null) }

    // Scroll to bottom on messages update
    LaunchedEffect(messages.size, searchLogs.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MidnightBg,
                drawerContentColor = TextPrimary
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHANNELS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = NeonJade
                        )
                    )
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.createNewThread("Vibe Space ✨")
                                drawerState.close()
                            }
                        },
                        modifier = Modifier
                            .background(BorderColor, CircleShape)
                            .size(36.dp)
                            .testTag("create_channel_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add space", tint = NeonJade)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderColor)

                LazyColumn(
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1f)
                ) {
                    items(threads) { thread ->
                        val isSelected = thread.id == activeThreadId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BorderColor else Color.Transparent)
                                .clickable {
                                    viewModel.selectThread(thread.id)
                                    coroutineScope.launch { drawerState.close() }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Vibe space icon",
                                    tint = if (isSelected) NeonJade else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = thread.title,
                                    maxLines = 1,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                            if (isSelected) {
                                IconButton(
                                    onClick = { viewModel.deleteThread(thread.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete space",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderColor)
                // Footer settings button inside drawer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            onNavigateToSettings()
                            coroutineScope.launch { drawerState.close() }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Connection Panel", color = TextSecondary)
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        val activeThread = threads.find { it.id == activeThreadId }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = activeThread?.title ?: "AI VibeChat",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = "Powered by ${viewModel.settingsRepository.provider} (${viewModel.settingsRepository.geminiModel})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NeonJade,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MidnightBg,
                        titleContentColor = TextPrimary,
                        navigationIconContentColor = TextPrimary,
                        actionIconContentColor = TextPrimary
                    )
                )
            },
            containerColor = MidnightBg
        ) { padding ->
            Row(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Left Column: Chat Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Quick stats / Info indicator when empty chats
                    if (messages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "AI Brain",
                                    tint = NeonJade,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Launch a Vibe Chat Session",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Configure endpoints, toggle agent modes, and look closer at responses with the Artifact renderer.",
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                        ) {
                            items(messages) { message ->
                                ChatMessageBubble(
                                    message = message,
                                    onCitationClick = { citation ->
                                        selectedCitationSource = citation
                                    }
                                )
                            }

                            // Dynamic Simulated Crawler Logs Module
                            if (isGenerating && searchLogs.isNotEmpty() && appMode == "search") {
                                item {
                                    SearchProgressTerminal(logs = searchLogs)
                                }
                            }

                            // Assistant Thinking / Streaming effect loader
                            if (isGenerating && searchLogs.isEmpty()) {
                                item {
                                    GeneratingIndicatorBox(appMode)
                                }
                            }
                        }
                    }

                    // Bottom Interface panel containing Quick Tabs & Text Field
                    BottomInputWorkspace(
                        text = textInput,
                        onValueChange = { textInput = it },
                        onSend = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            }
                        },
                        isGenerating = isGenerating,
                        currentMode = appMode,
                        onModeSelect = { viewModel.setAppMode(it) }
                    )
                }

                // Split Column Panel: Interactive Monospaced Artifacts Visualizer
                if (activeArtifact != null) {
                    val artifact = activeArtifact!!
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(360.dp)
                            .background(CarbonCard)
                            .border(1.dp, BorderColor, RoundedCornerShape(topStart = 16.dp))
                            .animateContentSize()
                    ) {
                        ArtifactPanel(
                            artifact = artifact,
                            onClose = { viewModel.clearArtifact() }
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet dialog for Citations info panel
    if (selectedCitationSource != null) {
        CitationDetailDialog(
            source = selectedCitationSource!!,
            onDismiss = { selectedCitationSource = null }
        )
    }
}

// Dynamic text formatter to render clickable [1], [2] citation pins beautifully
@Composable
fun FormattedTextWithCitations(
    text: String,
    sourcesJson: String?,
    onCitationClick: (CitationSource) -> Unit
) {
    if (sourcesJson == null) {
        SelectionContainer {
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
        return
    }

    // Parse citation details
    val sourceList = remember(sourcesJson) {
        val list = mutableListOf<CitationSource>()
        try {
            val array = JSONArray(sourcesJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CitationSource(
                        index = obj.getInt("index"),
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        snippet = obj.getString("snippet")
                    )
                )
            }
        } catch (e: Exception) {
            // Safe fallback
        }
        list
    }

    SelectionContainer {
        Column {
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            // Dynamic citation cards below response
            if (sourceList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "SOURCES USED:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonJade,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sourceList.forEach { citation ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BorderColor)
                                .clickable { onCitationClick(citation) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(NeonJade, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${citation.index}",
                                    color = Color(0xFF002D22),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = citation.title,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                modifier = Modifier.widthIn(max = 100.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: Message,
    onCitationClick: (CitationSource) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isThinkingExpanded by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.widthIn(max = 500.dp),
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
        ) {
            // 1. User/Assistant small row tag details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
            ) {
                Icon(
                    imageVector = if (message.isUser) Icons.Default.Person else Icons.Default.Psychology,
                    contentDescription = null,
                    tint = if (message.isUser) BrilliantCyan else NeonJade,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (message.isUser) "YOU" else "VIBE AGENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (message.isUser) BrilliantCyan else NeonJade,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // 2. Main Visual bubble surface
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isUser) BorderColor else CarbonCard
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (message.isUser) BrilliantCyan.copy(alpha = 0.4f) else BorderColor
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {

                    // 2a. Expandable thoughts section for Deep Reasoning mode response
                    if (message.thinkingText != null && !message.isUser) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MidnightBg)
                                .border(1.dp, NebulaViolet.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isThinkingExpanded = !isThinkingExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Memory,
                                        contentDescription = "Thought space",
                                        tint = NebulaViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Reasoning Process (" + message.thinkingText.length + " tokens spent)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NebulaViolet
                                    )
                                }
                                Icon(
                                    imageVector = if (isThinkingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle thinking",
                                    tint = NebulaViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (isThinkingExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                SelectionContainer {
                                    Text(
                                        text = message.thinkingText,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }

                    // 2b. Core Text Output (styled with customized citations if present)
                    FormattedTextWithCitations(
                        text = message.text,
                        sourcesJson = message.searchSourcesJson,
                        onCitationClick = onCitationClick
                    )

                    // 2c. Small Hover Toolbar action buttons inside card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(message.text))
                                Toast.makeText(context, "Copied text content", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy message",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simulated active crawler progress logger block (Terminal style!)
@Composable
fun SearchProgressTerminal(logs: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BrilliantCyan.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = "Network search module",
                        tint = BrilliantCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "VIBE_SEARCH_AGENT_LOGS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrilliantCyan
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(BrilliantCyan, CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            logs.forEach { log ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "❯ $log",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

// Gorgeous pulsating/blinking wave indicator for loading LLM completion
@Composable
fun GeneratingIndicatorBox(mode: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CarbonCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val pulseWidth by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 18f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = NeonJade.copy(alpha = 0.2f),
                        radius = pulseWidth * 1.5f + 10f,
                        center = center
                    )
                    drawCircle(
                        color = NeonJade,
                        radius = 6f,
                        center = center
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Vibe agent synthesizing output using $mode parameters...",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

// Capsules toggles for "Agent modes" above input
@Composable
fun BottomInputWorkspace(
    text: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isGenerating: Boolean,
    currentMode: String,
    onModeSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MidnightBg)
            .padding(12.dp)
    ) {
        // Mode Selector Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                AgentModeItem("standard", "Standard Mode", Icons.Default.ChatBubble, NeonJade),
                AgentModeItem("reasoning", "Deep Reasoning", Icons.Default.Psychology, NebulaViolet),
                AgentModeItem("search", "Web Search", Icons.Default.Search, BrilliantCyan)
            ).forEach { item ->
                val selected = currentMode == item.id
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) item.accentColor.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (selected) item.accentColor else BorderColor,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onModeSelect(item.id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) item.accentColor else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.label,
                        color = if (selected) TextPrimary else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Row Input and Trigger Icon
        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = text,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text"),
                placeholder = {
                    Text(
                        "Send a command or prompt...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonJade,
                    focusedBorderColor = NeonJade,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = CarbonCard,
                    unfocusedContainerColor = CarbonCard
                ),
                maxLines = 4,
                enabled = !isGenerating
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .background(
                        if (text.isNotBlank() && !isGenerating) NeonJade else BorderColor,
                        RoundedCornerShape(12.dp)
                    )
                    .size(48.dp)
                    .testTag("submit_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank() && !isGenerating) Color(0xFF002D22) else TextSecondary
                )
            }
        }
    }
}

data class AgentModeItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color
)

data class CitationSource(
    val index: Int,
    val title: String,
    val url: String,
    val snippet: String
)

// Monospaced Code Visualizer panel (Artifacts mode)
@Composable
fun ArtifactPanel(
    artifact: ArtifactData,
    onClose: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Upper header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MidnightBg)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACTIVE ARTIFACT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonJade,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = artifact.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier.widthIn(max = 200.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(artifact.code))
                    Toast.makeText(context, "Copied code block to clipboard!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", tint = TextSecondary)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close panel", tint = TextPrimary)
                }
            }
        }
        HorizontalDivider(color = BorderColor)

        // Monokai-dark styled code canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0B0D11))
                .padding(16.dp)
        ) {
            SelectionContainer {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = artifact.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = BrilliantCyan,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// Dialog explaining citation contents when clicked
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitationDetailDialog(
    source: CitationSource,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = NeonJade)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(NeonJade, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${source.index}",
                        color = Color(0xFF002D22),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Source Coordinates",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        },
        text = {
            Column {
                Text(
                    text = source.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = source.url,
                    color = NeonJade,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "REPRESENTATIVE SEGMENT:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\"${source.snippet}\"",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            }
        },
        containerColor = CarbonCard,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary
    )
}
