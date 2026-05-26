package com.example.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DocDao
import com.example.data.Document
import com.example.data.Page
import com.example.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentViewModel(private val dao: DocDao, private val context: Context) : ViewModel() {
    val allDocuments: Flow<List<Document>> = dao.getAllDocuments()
    val favoriteDocuments: Flow<List<Document>> = dao.getFavoriteDocuments()
    val recentDocuments: Flow<List<Document>> = dao.getRecentDocuments()

    fun createDocumentFromUris(
        uris: List<Uri>,
        onCreated: (Long) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val copiedFiles = uris.mapNotNull { uri -> FileUtils.copyUriToImageFile(context, uri) }
            if (copiedFiles.isEmpty()) {
                withContext(Dispatchers.Main) { onError("No gallery images could be imported") }
                return@launch
            }

            val now = System.currentTimeMillis()
            val docId = dao.insertDocument(
                Document(
                    title = "Imported ${copiedFiles.size} page${if (copiedFiles.size == 1) "" else "s"}",
                    timestamp = now,
                    updatedAt = now,
                    type = "IMAGE"
                )
            )
            copiedFiles.forEachIndexed { index, file ->
                dao.insertPage(Page(documentId = docId, imagePath = file.absolutePath, pageOrder = index))
            }
            withContext(Dispatchers.Main) { onCreated(docId) }
        }
    }

    fun renameDocument(id: Long, title: String) {
        val safeTitle = title.trim().ifBlank { "Untitled Scan" }
        viewModelScope.launch(Dispatchers.IO) { dao.renameDocument(id, safeTitle) }
    }

    fun toggleFavorite(document: Document) {
        viewModelScope.launch(Dispatchers.IO) { dao.setFavorite(document.id, !document.isFavorite) }
    }

    fun deleteDocument(id: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteDocumentById(id)
            withContext(Dispatchers.Main) { onDeleted() }
        }
    }
}

class DocumentViewModelFactory(private val dao: DocDao, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocumentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DocumentViewModel(dao, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
