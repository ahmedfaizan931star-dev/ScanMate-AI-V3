package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppDatabase
import com.example.data.DocumentWithPages
import com.example.data.Page
import com.example.utils.FileUtils
import com.example.utils.OcrHelper
import com.example.utils.PdfExportQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    docId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToPageEditor: (Long, Long) -> Unit,
    onNavigateToSignature: (Long) -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).docDao() }
    val docFlow = remember(docId) { dao.getDocumentWithPages(docId) }
    val documentWithPages by docFlow.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMetaDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedPdf by remember { mutableStateOf<File?>(null) }
    var renameTitle by remember { mutableStateOf("") }
    var exportName by remember { mutableStateOf("ScanMate_${docId}_${System.currentTimeMillis()}") }
    var category by remember { mutableStateOf("General") }
    var tags by remember { mutableStateOf("") }
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    LaunchedEffect(documentWithPages?.document?.title, documentWithPages?.document?.category, documentWithPages?.document?.tags) {
        renameTitle = documentWithPages?.document?.title.orEmpty()
        category = documentWithPages?.document?.category ?: "General"
        tags = documentWithPages?.document?.tags.orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(documentWithPages?.document?.title ?: "Document", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    documentWithPages?.let { dwp ->
                        IconButton(onClick = {
                            coroutineScope.launch(Dispatchers.IO) { dao.setFavorite(dwp.document.id, !dwp.document.isFavorite) }
                        }) {
                            Icon(if (dwp.document.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite")
                        }
                    }
                    IconButton(onClick = { showRenameDialog = true }) { Icon(Icons.Default.DriveFileRenameOutline, "Rename") }
                    IconButton(onClick = { showExportDialog = true }) { Icon(Icons.Default.PictureAsPdf, "Export PDF") }
                    IconButton(onClick = { extractOcr(documentWithPages, context, dao, clipboardManager) { isProcessing = it } }) {
                        Icon(Icons.AutoMirrored.Filled.TextSnippet, "OCR full document")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Delete") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            if (isProcessing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            val dwp = documentWithPages
            if (dwp == null) {
                LoadingDocumentState()
            } else {
                val pages = dwp.pages.sortedBy { it.pageOrder }
                DocumentPreview(pages)
                QuickActionRow(
                    dwp = dwp,
                    onShareFirstImage = {
                        val firstFile = pages.firstOrNull()?.imagePath?.let { File(it) }
                        if (firstFile != null && firstFile.exists()) {
                            FileUtils.shareFile(context, firstFile, FileUtils.mimeTypeFor(firstFile))
                        } else {
                            Toast.makeText(context, "No image file found to share", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onExport = { showExportDialog = true },
                    onSignature = { onNavigateToSignature(docId) },
                    onMeta = { showMetaDialog = true }
                )
                DocumentMetaChips(dwp)
                OcrCard(dwp = dwp, clipboardManager = clipboardManager, context = context)
                PageThumbnails(pages, onEdit = { page -> onNavigateToPageEditor(docId, page.id) })
                PageManagementList(
                    pages = pages,
                    onEdit = { page -> onNavigateToPageEditor(docId, page.id) },
                    onMove = { page, direction ->
                        coroutineScope.launch {
                            movePage(dao, docId, page, direction)
                            Toast.makeText(context, "Page order updated", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDuplicate = { page ->
                        coroutineScope.launch {
                            duplicatePage(context, dao, docId, page)
                            Toast.makeText(context, "Page duplicated", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDelete = { page ->
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                dao.deletePageById(page.id)
                                renumberPages(dao, docId)
                            }
                            Toast.makeText(context, "Page deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename document") },
            text = {
                OutlinedTextField(
                    value = renameTitle,
                    onValueChange = { renameTitle = it },
                    singleLine = true,
                    label = { Text("Document name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch(Dispatchers.IO) { dao.renameDocument(docId, renameTitle.trim().ifBlank { "Untitled Scan" }) }
                    showRenameDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showMetaDialog) {
        AlertDialog(
            onDismissRequest = { showMetaDialog = false },
            title = { Text("Category & tags") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("Tags, comma separated") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch(Dispatchers.IO) { dao.updateCategoryTags(docId, category.ifBlank { "General" }, tags) }
                    showMetaDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showMetaDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this document?") },
            text = { Text("This removes the document record from ScanMate. Export or share anything important first.") },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        dao.deleteDocumentById(docId)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export PDF") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = exportName,
                        onValueChange = { exportName = it },
                        label = { Text("PDF name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PdfExportQuality.entries.forEach { quality ->
                        OutlinedButton(onClick = {
                            showExportDialog = false
                            exportPdf(documentWithPages, context, quality, exportName, onPdfReady = { exportedPdf = it }) { isProcessing = it }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                Text(quality.label, fontWeight = FontWeight.Bold)
                                Text(quality.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text("Cancel") } }
        )
    }

    exportedPdf?.let { pdfFile ->
        AlertDialog(
            onDismissRequest = { exportedPdf = null },
            title = { Text("PDF ready") },
            text = { Text("${pdfFile.name} was exported successfully and can be opened or shared.") },
            confirmButton = {
                Button(onClick = { FileUtils.openFile(context, pdfFile, "application/pdf") }) { Text("Open PDF") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { FileUtils.shareFile(context, pdfFile, "application/pdf") }) { Text("Share") }
                    TextButton(onClick = { exportedPdf = null }) { Text("Close") }
                }
            }
        )
    }
}

@Composable
private fun LoadingDocumentState() {
    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text("Loading document...")
    }
}

@Composable
private fun DocumentPreview(pages: List<Page>) {
    val firstPath = pages.firstOrNull()?.imagePath
    val bitmap = remember(firstPath) { firstPath?.let { FileUtils.decodeSampledBitmap(it, 1400, 1400) } }
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Document Page",
                modifier = Modifier.fillMaxWidth().height(420.dp).padding(12.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth().height(220.dp).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.ImageNotSupported, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No preview available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    dwp: DocumentWithPages,
    onShareFirstImage: () -> Unit,
    onExport: () -> Unit,
    onSignature: () -> Unit,
    onMeta: () -> Unit
) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { AssistChip(onClick = {}, label = { Text("${dwp.pages.size} page${if (dwp.pages.size == 1) "" else "s"}") }) }
        item { AssistChip(onClick = onShareFirstImage, leadingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp)) }, label = { Text("Share image") }) }
        item { AssistChip(onClick = onExport, leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp)) }, label = { Text("Export PDF") }) }
        item { AssistChip(onClick = onSignature, leadingIcon = { Icon(Icons.Default.Style, null, modifier = Modifier.size(16.dp)) }, label = { Text("Signature") }) }
        item { AssistChip(onClick = onMeta, leadingIcon = { Icon(Icons.Default.Tag, null, modifier = Modifier.size(16.dp)) }, label = { Text("Tags") }) }
    }
}

@Composable
private fun DocumentMetaChips(dwp: DocumentWithPages) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterChip(selected = true, onClick = {}, label = { Text(dwp.document.category.ifBlank { "General" }) }) }
        val tagList = dwp.document.tags.split(',').map { it.trim() }.filter { it.isNotBlank() }
        tagList.forEach { tag ->
            item { FilterChip(selected = false, onClick = {}, label = { Text(tag) }) }
        }
    }
}

@Composable
private fun OcrCard(dwp: DocumentWithPages, clipboardManager: ClipboardManager, context: Context) {
    val text = dwp.document.ocrText
    if (!text.isNullOrBlank()) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Extracted Text", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("Extracted Text", text))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy") }
                    TextButton(onClick = { FileUtils.shareText(context, text) }) { Text("Share") }
                    TextButton(onClick = {
                        kotlinx.coroutines.MainScope().launch {
                            val file = FileUtils.saveTextFile(context, text, "OCR_${dwp.document.id}_${System.currentTimeMillis()}")
                            if (file != null) FileUtils.shareFile(context, file, "text/plain") else Toast.makeText(context, "TXT export failed", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Save TXT") }
                }
            }
        }
    }
}

@Composable
private fun PageThumbnails(pages: List<Page>, onEdit: (Page) -> Unit) {
    if (pages.isNotEmpty()) {
        Text("Pages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        LazyRow(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(pages, key = { it.id }) { page ->
                val bitmap = remember(page.imagePath) { FileUtils.decodeSampledBitmap(page.imagePath, 360, 360) }
                Card(onClick = { onEdit(page) }, shape = RoundedCornerShape(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (bitmap != null) {
                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Thumbnail", modifier = Modifier.size(112.dp), contentScale = ContentScale.Crop)
                        } else {
                            Column(modifier = Modifier.size(112.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.ImageNotSupported, null)
                            }
                        }
                        Text("Page ${page.pageOrder + 1}", modifier = Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PageManagementList(
    pages: List<Page>,
    onEdit: (Page) -> Unit,
    onMove: (Page, Int) -> Unit,
    onDuplicate: (Page) -> Unit,
    onDelete: (Page) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Page management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        pages.forEach { page ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Page ${page.pageOrder + 1}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onMove(page, -1) }) { Icon(Icons.Default.KeyboardArrowUp, "Move up") }
                    IconButton(onClick = { onMove(page, 1) }) { Icon(Icons.Default.KeyboardArrowDown, "Move down") }
                    IconButton(onClick = { onDuplicate(page) }) { Icon(Icons.Default.ContentCopy, "Duplicate") }
                    IconButton(onClick = { onEdit(page) }) { Icon(Icons.Default.Edit, "Edit") }
                    IconButton(onClick = { onDelete(page) }) { Icon(Icons.Default.Delete, "Delete") }
                }
            }
        }
    }
}

private fun exportPdf(
    dwp: DocumentWithPages?,
    context: Context,
    quality: PdfExportQuality,
    filename: String,
    onPdfReady: (File) -> Unit,
    setProcessing: (Boolean) -> Unit
) {
    if (dwp == null) return
    val pages = dwp.pages.sortedBy { it.pageOrder }
    if (pages.isEmpty()) {
        Toast.makeText(context, "No pages found to export", Toast.LENGTH_SHORT).show()
        return
    }
    kotlinx.coroutines.MainScope().launch {
        setProcessing(true)
        val pdfFile = FileUtils.generatePdfFromPaths(
            context = context,
            imagePaths = pages.map { it.imagePath },
            filename = filename.ifBlank { "ScanMate_${dwp.document.id}_${System.currentTimeMillis()}" },
            quality = quality
        )
        setProcessing(false)
        if (pdfFile != null) {
            Toast.makeText(context, "PDF exported", Toast.LENGTH_SHORT).show()
            onPdfReady(pdfFile)
        } else {
            Toast.makeText(context, "PDF export failed. Check that pages are valid images.", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun extractOcr(
    dwp: DocumentWithPages?,
    context: Context,
    dao: com.example.data.DocDao,
    clipboardManager: ClipboardManager,
    setProcessing: (Boolean) -> Unit
) {
    if (dwp == null || dwp.pages.isEmpty()) {
        Toast.makeText(context, "No pages available for OCR", Toast.LENGTH_SHORT).show()
        return
    }
    val pages = dwp.pages.sortedBy { it.pageOrder }
    kotlinx.coroutines.MainScope().launch {
        setProcessing(true)
        val text = withContext(Dispatchers.IO) {
            pages.mapIndexedNotNull { index, page ->
                val file = File(page.imagePath)
                if (!file.exists() || file.length() == 0L) return@mapIndexedNotNull null
                val fileResult = OcrHelper.extractTextFromFile(context, file)
                val result = if (fileResult.startsWith("OCR failed", ignoreCase = true)) {
                    FileUtils.decodeSampledBitmap(file.absolutePath, 1800, 1800)?.let { bitmap ->
                        OcrHelper.extractTextFromBitmap(bitmap)
                    }.orEmpty()
                } else {
                    fileResult
                }
                if (result.isBlank() || result.startsWith("OCR failed", ignoreCase = true)) null else "Page ${index + 1}:\n$result"
            }.joinToString(separator = "\n\n")
        }
        setProcessing(false)
        if (text.isBlank()) {
            Toast.makeText(context, "No readable text found", Toast.LENGTH_SHORT).show()
            return@launch
        }
        withContext(Dispatchers.IO) { dao.updateOcrText(dwp.document.id, text) }
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Extracted Text", text))
        Toast.makeText(context, "OCR completed and copied", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun duplicatePage(context: Context, dao: com.example.data.DocDao, docId: Long, page: Page) = withContext(Dispatchers.IO) {
    val copied = FileUtils.duplicateImageFile(context, page.imagePath) ?: return@withContext
    val pages = dao.getPagesForDocumentOnce(docId).sortedBy { it.pageOrder }
    val insertIndex = pages.indexOfFirst { it.id == page.id }.takeIf { it >= 0 }?.plus(1) ?: pages.size
    pages.forEachIndexed { index, existing ->
        val order = if (index >= insertIndex) index + 1 else index
        dao.updatePageOrder(existing.id, order)
    }
    dao.insertPage(Page(documentId = docId, imagePath = copied.absolutePath, pageOrder = insertIndex))
    renumberPages(dao, docId)
}

private suspend fun movePage(dao: com.example.data.DocDao, docId: Long, page: Page, direction: Int) = withContext(Dispatchers.IO) {
    val pages = dao.getPagesForDocumentOnce(docId).sortedBy { it.pageOrder }.toMutableList()
    val index = pages.indexOfFirst { it.id == page.id }
    val newIndex = (index + direction).coerceIn(0, pages.lastIndex)
    if (index < 0 || index == newIndex) return@withContext
    val current = pages.removeAt(index)
    pages.add(newIndex, current)
    pages.forEachIndexed { order, existing -> dao.updatePageOrder(existing.id, order) }
}

private suspend fun renumberPages(dao: com.example.data.DocDao, docId: Long) {
    dao.getPagesForDocumentOnce(docId).sortedBy { it.pageOrder }.forEachIndexed { index, page ->
        dao.updatePageOrder(page.id, index)
    }
}
