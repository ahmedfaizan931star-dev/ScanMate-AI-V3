package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SettingsRepository
import com.example.domain.GeminiHelper
import com.example.domain.GeminiModels
import com.example.utils.FileUtils
import com.example.utils.NetworkUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val apiKey by repository.geminiApiKeyFlow.collectAsState(initial = "")
    val selectedModelId by repository.geminiModelIdFlow.collectAsState(initial = GeminiModels.DEFAULT_MODEL_ID)
    val selectedModel = GeminiModels.optionFor(selectedModelId)
    val isOnline = NetworkUtils.isOnline(context)

    var prompt by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var responseIsError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val aiTools = listOf(
        "Summarize Document" to "Summarize this document in clear bullet points:",
        "Generate MCQs" to "Create multiple choice questions with answers from this text:",
        "Clean OCR Text" to "Clean this OCR text, fix spacing, and preserve meaning:",
        "Study Notes" to "Convert this into exam-ready study notes:",
        "Action Items" to "Extract key action items from this text:"
    )

    fun showLocalError(message: String) {
        response = message
        responseIsError = true
    }

    fun runGeminiRequest(finalPrompt: String) {
        when {
            apiKey.isNullOrBlank() -> showLocalError("Add Gemini API key in Settings to use AI features.")
            !NetworkUtils.isOnline(context) -> showLocalError("AI needs internet. Offline tools still work.")
            finalPrompt.isBlank() -> showLocalError("Paste OCR text or type a prompt first.")
            else -> coroutineScope.launch {
                isLoading = true
                response = ""
                val result = GeminiHelper(apiKey.orEmpty()).generateContent(finalPrompt, selectedModelId)
                response = result.text
                responseIsError = !result.isSuccess
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AssistChip(
                onClick = {},
                leadingIcon = { Icon(if (isOnline) Icons.Default.AutoAwesome else Icons.Default.CloudOff, null) },
                label = {
                    Text(
                        if (isOnline) "Online · ${selectedModel.displayName} selected"
                        else "AI needs internet. Offline tools still work."
                    )
                }
            )

            if (apiKey.isNullOrBlank()) {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("API key required", fontWeight = FontWeight.Bold)
                        Text("Add Gemini API key in Settings to use AI features.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Paste OCR text or ask AI...") },
                supportingText = { Text("Quick actions use this text. Your key stays in Settings only.") },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                maxLines = 6,
                enabled = !isLoading,
                shape = RoundedCornerShape(18.dp)
            )

            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(aiTools) { (tool, template) ->
                    OutlinedButton(
                        onClick = {
                            val sourceText = prompt.trim()
                            if (sourceText.isBlank()) {
                                showLocalError("Paste OCR text or type a prompt first.")
                            } else {
                                runGeminiRequest("$template\n\n$sourceText")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) { Text(tool) }
                }

                if (response.isNotBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (responseIsError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    if (responseIsError) "Needs attention" else "AI Response",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(response, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        clipboardManager.setPrimaryClip(ClipData.newPlainText("AI Response", response))
                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, null)
                                        Text("Copy")
                                    }
                                    TextButton(onClick = { FileUtils.shareText(context, response) }) {
                                        Icon(Icons.Default.Share, null)
                                        Text("Share")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { runGeminiRequest(prompt.trim()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) { Text(if (isLoading) "Generating..." else "Generate") }
        }
    }
}
