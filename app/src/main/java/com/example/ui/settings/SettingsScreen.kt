package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsRepository
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsRepository: SettingsRepository, onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    // Local states mirroring SharedPreferences values
    var selectedProvider by remember { mutableStateOf(settingsRepository.provider) }
    var geminiKey by remember { mutableStateOf(settingsRepository.geminiKey) }
    var geminiModel by remember { mutableStateOf(settingsRepository.geminiModel) }

    var openAIKey by remember { mutableStateOf(settingsRepository.openAIKey) }
    var openAIModel by remember { mutableStateOf(settingsRepository.openAIModel) }
    var openAIUrl by remember { mutableStateOf(settingsRepository.openAIUrl) }

    var deepseekKey by remember { mutableStateOf(settingsRepository.deepseekKey) }
    var deepseekModel by remember { mutableStateOf(settingsRepository.deepseekModel) }
    var deepseekUrl by remember { mutableStateOf(settingsRepository.deepseekUrl) }

    var customKey by remember { mutableStateOf(settingsRepository.customKey) }
    var customModel by remember { mutableStateOf(settingsRepository.customModel) }
    var customUrl by remember { mutableStateOf(settingsRepository.customUrl) }

    var temperature by remember { mutableStateOf(settingsRepository.temperature) }

    var isKeyVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connection Panel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightBg,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = MidnightBg
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Introductory card
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = NeonJade)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "MULTI-ENDPOINT CONFIGURATION",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonJade,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Pair your interactive workspace client-side with any OpenAI-compatible custom gateway, local LM instance, or cloud-hosted cognitive endpoints.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Provider Choices Segment
            Text(
                "SELECT ACTIVE LLM PROVIDER",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            listOf("Gemini", "OpenAI", "DeepSeek", "Custom").forEach { prov ->
                val isSelected = selectedProvider == prov
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) BorderColor else Color.Transparent)
                        .clickable { selectedProvider = prov }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        prov + if (prov == "Gemini") " (AI Studio native)" else "",
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                    RadioButton(
                        selected = isSelected,
                        onClick = { selectedProvider = prov },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = NeonJade,
                            unselectedColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic Form based on selection
            Text(
                "PROVIDER SETTINGS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = textProviderModeColor(selectedProvider),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderProviderModeColor(selectedProvider)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (selectedProvider) {
                        "Gemini" -> {
                            Text(
                                "Uses direct REST services. If the API Key field is left blank, the client automatically defaults safely to the pre-configured key located secure in Google AI Studio's sandbox.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            CustomSettingsField(
                                value = geminiModel,
                                onValueChange = { geminiModel = it },
                                label = "Model Name",
                                placeholder = "gemini-2.5-flash"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CustomKeyField(
                                value = geminiKey,
                                onValueChange = { geminiKey = it },
                                isVisible = isKeyVisible,
                                onVisibilityToggle = { isKeyVisible = !isKeyVisible }
                            )
                        }
                        "OpenAI" -> {
                            CustomSettingsField(
                                value = openAIUrl,
                                onValueChange = { openAIUrl = it },
                                label = "API Base URL",
                                placeholder = "https://api.openai.com/v1/"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CustomSettingsField(
                                value = openAIModel,
                                onValueChange = { openAIModel = it },
                                label = "Model Name",
                                placeholder = "gpt-4o-mini"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CustomKeyField(
                                value = openAIKey,
                                onValueChange = { openAIKey = it },
                                isVisible = isKeyVisible,
                                onVisibilityToggle = { isKeyVisible = !isKeyVisible }
                            )
                        }
                        "DeepSeek" -> {
                            Text(
                                "Using OpenAI-compatible endpoints directly connected with Global Cloud servers.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            CustomSettingsField(
                                value = deepseekUrl,
                                onValueChange = { deepseekUrl = it },
                                label = "API Base URL",
                                placeholder = "https://api.deepseek.com/v1/"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CustomSettingsField(
                                value = deepseekModel,
                                onValueChange = { deepseekModel = it },
                                label = "Model Name",
                                placeholder = "deepseek-chat"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CustomKeyField(
                                value = deepseekKey,
                                onValueChange = { deepseekKey = it },
                                isVisible = isKeyVisible,
                                onVisibilityToggle = { isKeyVisible = !isKeyVisible }
                            )
                        }
                        else -> { // Custom API
                            CustomSettingsField(
                                value = customUrl,
                                onValueChange = { customUrl = it },
                                label = "Target Gateway Endpoint",
                                placeholder = "https://api.example.com/v1/"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CustomSettingsField(
                                value = customModel,
                                onValueChange = { customModel = it },
                                label = "Target Model ID",
                                placeholder = "custom-llm-v1"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CustomKeyField(
                                value = customKey,
                                onValueChange = { customKey = it },
                                isVisible = isKeyVisible,
                                onVisibilityToggle = { isKeyVisible = !isKeyVisible }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Temperature slider
                    Text(
                        "INFERENCE TEMPERATURE: ${String.format("%.2f", temperature)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 0.0f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonJade,
                            activeTrackColor = NeonJade,
                            inactiveTrackColor = BorderColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom CTA Call action button
            Button(
                onClick = {
                    // Update SharedPreferences in state
                    settingsRepository.provider = selectedProvider
                    settingsRepository.geminiKey = geminiKey
                    settingsRepository.geminiModel = geminiModel

                    settingsRepository.openAIKey = openAIKey
                    settingsRepository.openAIModel = openAIModel
                    settingsRepository.openAIUrl = openAIUrl

                    settingsRepository.deepseekKey = deepseekKey
                    settingsRepository.deepseekModel = deepseekModel
                    settingsRepository.deepseekUrl = deepseekUrl

                    settingsRepository.customKey = customKey
                    settingsRepository.customModel = customModel
                    settingsRepository.customUrl = customUrl

                    settingsRepository.temperature = temperature

                    Toast.makeText(context, "Network configurations locked and saved! ⚡", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_config_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonJade,
                    contentColor = Color(0xFF002D22)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "SAVE CONFIGURATION",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun CustomSettingsField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        placeholder = { Text(placeholder, color = TextSecondary.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = NeonJade,
            unfocusedBorderColor = BorderColor,
            focusedContainerColor = MidnightBg,
            unfocusedContainerColor = MidnightBg
        ),
        singleLine = true
    )
}

@Composable
fun CustomKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("AI API Private Authorization Key (Bearer)", color = TextSecondary) },
        placeholder = { Text("sk-...", color = TextSecondary.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = NeonJade,
            unfocusedBorderColor = BorderColor,
            focusedContainerColor = MidnightBg,
            unfocusedContainerColor = MidnightBg
        ),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = TextSecondary) },
        trailingIcon = {
            IconButton(onClick = onVisibilityToggle) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle password visibility",
                    tint = TextSecondary
                )
            }
        },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation()
    )
}

private fun textProviderModeColor(prov: String): Color {
    return when (prov) {
        "Gemini" -> NeonJade
        "OpenAI" -> BrilliantCyan
        "DeepSeek" -> NebulaViolet
        else -> TextSecondary
    }
}

private fun borderProviderModeColor(prov: String): Color {
    return when (prov) {
        "Gemini" -> NeonJade.copy(alpha = 0.4f)
        "OpenAI" -> BrilliantCyan.copy(alpha = 0.4f)
        "DeepSeek" -> NebulaViolet.copy(alpha = 0.4f)
        else -> BorderColor
    }
}
