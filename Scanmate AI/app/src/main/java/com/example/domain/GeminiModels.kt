package com.example.domain

data class GeminiModelOption(
    val displayName: String,
    val modelId: String,
    val description: String,
    val isPreview: Boolean = false
)

object GeminiModels {
    const val DEFAULT_MODEL_ID = "gemini-2.5-flash"

    val options = listOf(
        GeminiModelOption(
            displayName = "Gemini 2.5 Flash",
            modelId = "gemini-2.5-flash",
            description = "Recommended default for fast document summaries, OCR cleanup and study notes."
        ),
        GeminiModelOption(
            displayName = "Gemini 2.5 Pro",
            modelId = "gemini-2.5-pro",
            description = "Higher quality for longer or more complex documents."
        ),
        GeminiModelOption(
            displayName = "Gemini 3.5 Flash",
            modelId = "gemini-3.5-flash",
            description = "Newest Flash option where enabled for your API key."
        ),
        GeminiModelOption(
            displayName = "Gemini 3 Flash Preview",
            modelId = "gemini-3-flash-preview",
            description = "Preview 3-series Flash model; availability depends on your API key/project.",
            isPreview = true
        ),
        GeminiModelOption(
            displayName = "Gemini 3.1 Pro Preview",
            modelId = "gemini-3.1-pro-preview",
            description = "Preview 3-series Pro model; availability depends on your API key/project.",
            isPreview = true
        ),
        GeminiModelOption(
            displayName = "Gemini 3.1 Flash-Lite",
            modelId = "gemini-3.1-flash-lite",
            description = "Efficient 3-series model for low-cost quick tasks."
        )
    )

    fun optionFor(modelId: String?): GeminiModelOption =
        options.firstOrNull { it.modelId == modelId } ?: options.first { it.modelId == DEFAULT_MODEL_ID }

    fun modelIdOrDefault(modelId: String?): String = optionFor(modelId).modelId

    fun isKnown(modelId: String?): Boolean = options.any { it.modelId == modelId }
}
