package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.Document
import com.example.ui.theme.GridArchiveBg
import com.example.ui.theme.GridArchiveIcon
import com.example.ui.theme.GridOcrBg
import com.example.ui.theme.GridOcrIcon
import com.example.ui.theme.GridPdfBg
import com.example.ui.theme.GridPdfIcon
import com.example.ui.viewmodels.DocumentViewModel
import com.example.ui.viewmodels.DocumentViewModelFactory
import com.example.utils.NetworkUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToDoc: (Long) -> Unit,
    onNavigateToQr: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToZip: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToPdfTools: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).docDao() }
    val viewModel: DocumentViewModel = viewModel(factory = DocumentViewModelFactory(dao, context))
    val documents by viewModel.allDocuments.collectAsState(initial = emptyList())
    val favorites by viewModel.favoriteDocuments.collectAsState(initial = emptyList())
    val isOnline = remember { NetworkUtils.isOnline(context) }
    var query by remember { mutableStateOf("") }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(DocumentSortMode.UPDATED) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 30)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.createDocumentFromUris(
                uris = uris,
                onCreated = { newDocId -> onNavigateToDoc(newDocId) },
                onError = { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    val visibleDocuments = remember(documents, query, showFavoritesOnly, sortMode) {
        documents.filter { doc ->
            val matchesQuery = query.isBlank() ||
                doc.title.contains(query, ignoreCase = true) ||
                doc.ocrText.orEmpty().contains(query, ignoreCase = true) ||
                doc.category.contains(query, ignoreCase = true) ||
                doc.tags.contains(query, ignoreCase = true)
            val matchesFavorite = !showFavoritesOnly || doc.isFavorite
            matchesQuery && matchesFavorite
        }.let { filtered ->
            when (sortMode) {
                DocumentSortMode.UPDATED -> filtered.sortedByDescending { it.updatedAt }
                DocumentSortMode.NAME -> filtered.sortedBy { it.title.lowercase(Locale.getDefault()) }
                DocumentSortMode.TYPE -> filtered.sortedWith(compareBy<Document> { it.type }.thenByDescending { it.updatedAt })
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(84.dp).padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem("Home", Icons.Default.Home, true) { }
                    BottomNavItem("Files", Icons.Default.Folder, false, onNavigateToFiles)
                    BottomNavItem("Tools", Icons.Default.Apps, false, onNavigateToQr)
                    BottomNavItem("AI Ask", Icons.Default.AutoAwesome, false, onNavigateToAi)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                HeaderRow(onNavigateToAi, onNavigateToSettings)
            }

            item {
                OnlineStatusCard(isOnline = isOnline)
            }

            item {
                Card(
                    onClick = onNavigateToCamera,
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF4D8DFF)))
                            )
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("New Document Scan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("CameraX scan, OCR, PDF export, ZIP backup and optional AI tools.", color = Color.White.copy(alpha = 0.86f), fontSize = 14.sp, lineHeight = 19.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier.size(54.dp).background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, "Scan", tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DashboardStat("Docs", documents.size.toString(), Icons.Default.Description)
                    DashboardStat("Favorites", favorites.size.toString(), Icons.Default.Favorite)
                    DashboardStat("AI", if (isOnline) "Online" else "Offline", Icons.Default.AutoAwesome)
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ToolButton("PDF", Icons.Default.PictureAsPdf, GridPdfBg, GridPdfIcon) { onNavigateToPdfTools() }
                    ToolButton("OCR", Icons.Default.TextSnippet, GridOcrBg, GridOcrIcon) { onNavigateToCamera() }
                    ToolButton("QR", Icons.Default.QrCodeScanner, GridOcrBg, GridOcrIcon) { onNavigateToQr() }
                    ToolButton("ZIP", Icons.Default.FolderZip, GridArchiveBg, GridArchiveIcon) { onNavigateToZip() }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search documents or OCR text") },
                    shape = RoundedCornerShape(18.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !showFavoritesOnly,
                            onClick = { showFavoritesOnly = false },
                            label = { Text("All") }
                        )
                        FilterChip(
                            selected = showFavoritesOnly,
                            onClick = { showFavoritesOnly = true },
                            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Favorites") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = sortMode == DocumentSortMode.UPDATED, onClick = { sortMode = DocumentSortMode.UPDATED }, label = { Text("Latest") })
                        FilterChip(selected = sortMode == DocumentSortMode.NAME, onClick = { sortMode = DocumentSortMode.NAME }, label = { Text("Name") })
                        FilterChip(selected = sortMode == DocumentSortMode.TYPE, onClick = { sortMode = DocumentSortMode.TYPE }, label = { Text("Type") })
                    }
                }
            }

            if (visibleDocuments.isEmpty()) {
                item {
                    EmptyDocumentState(
                        hasDocuments = documents.isNotEmpty(),
                        onImport = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onScan = onNavigateToCamera
                    )
                }
            } else {
                item {
                    SectionHeader(if (showFavoritesOnly) "FAVORITE DOCUMENTS" else "RECENT FILES", "${visibleDocuments.size} shown")
                }
                items(visibleDocuments, key = { it.id }) { doc ->
                    DocumentListItem(
                        doc = doc,
                        onClick = { onNavigateToDoc(doc.id) },
                        onFavorite = { viewModel.toggleFavorite(doc) }
                    )
                }
            }

            item {
                Card(
                    onClick = onNavigateToAi,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, "AI", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("GEMINI ASSISTANT", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.05.em))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Optional online AI: summarize documents, clean OCR and generate study material.", style = MaterialTheme.typography.bodyMedium)
                        }
                        Icon(Icons.Default.ChevronRight, "Go", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private enum class DocumentSortMode { UPDATED, NAME, TYPE }

@Composable
private fun HeaderRow(onNavigateToAi: () -> Unit, onNavigateToSettings: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("PROFESSIONAL SUITE", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.16.em, color = MaterialTheme.colorScheme.primary))
            Text("ScanMate AI Pro", style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = MaterialTheme.colorScheme.onBackground))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onNavigateToAi, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(42.dp)) {
                Icon(Icons.Default.AutoAwesome, "AI", tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = onNavigateToSettings, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(42.dp)) {
                Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun OnlineStatusCard(isOnline: Boolean) {
    val label = if (isOnline) "Online tools ready" else "Offline safe mode"
    val detail = if (isOnline) "Offline Ready · OCR Ready · PDF Ready · AI works after Settings setup" else "Offline Ready · OCR Ready · PDF Ready · AI paused"
    AssistChip(
        onClick = {},
        label = { Text("$label · $detail") },
        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
    )
}

@Composable
private fun DashboardStat(label: String, value: String, icon: ImageVector) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.width(104.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
fun ToolButton(label: String, icon: ImageVector, bgColor: Color, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(64.dp).background(bgColor, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(25.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun SectionHeader(title: String, meta: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em, color = MaterialTheme.colorScheme.onSurfaceVariant))
        Text(meta, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun EmptyDocumentState(hasDocuments: Boolean, onImport: () -> Unit, onScan: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (hasDocuments) Icons.Default.Search else Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(if (hasDocuments) "No matching documents" else "No scans yet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                if (hasDocuments) "Try a different search term or remove the favorite filter." else "Start with a camera scan or import images from your gallery.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onImport) { Text("Import") }
                Button(onClick = onScan) { Text("Scan now") }
            }
        }
    }
}

@Composable
fun DocumentListItem(doc: Document, onClick: () -> Unit, onFavorite: () -> Unit) {
    val dateString = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(doc.timestamp))
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val iconBg = if (doc.type == "PDF") GridPdfBg else GridOcrBg
            val iconTint = if (doc.type == "PDF") GridPdfIcon else GridOcrIcon
            Box(modifier = Modifier.size(42.dp).background(iconBg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(if (doc.type == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Description, contentDescription = null, tint = iconTint)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(doc.title, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${doc.category} · $dateString", style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!doc.ocrText.isNullOrEmpty()) {
                AssistChip(onClick = {}, label = { Text("OCR") }, modifier = Modifier.height(32.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            IconButton(onClick = onFavorite) {
                Icon(if (doc.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = if (doc.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun RowScope.BottomNavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable { onClick() }) {
        Box(
            modifier = Modifier.padding(vertical = 4.dp).background(
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = CircleShape
            ).padding(horizontal = 18.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
