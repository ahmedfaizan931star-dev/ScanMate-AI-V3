package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.domain.GeminiModels

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode(val storageValue: String, val label: String, val description: String) {
    SYSTEM("system", "System / Device", "Follow the phone theme automatically"),
    LIGHT("light", "Light", "Always use the light theme"),
    DARK("dark", "Dark", "Always use the dark theme");

    companion object {
        fun fromStorage(value: String?): ThemeMode = entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

class SettingsRepository(private val context: Context) {

    companion object {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_MODEL_ID = stringPreferencesKey("gemini_model_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val geminiApiKeyFlow: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[GEMINI_API_KEY] }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data
        .map { preferences -> ThemeMode.fromStorage(preferences[THEME_MODE]) }

    val geminiModelIdFlow: Flow<String> = context.dataStore.data
        .map { preferences -> GeminiModels.modelIdOrDefault(preferences[GEMINI_MODEL_ID]) }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[GEMINI_API_KEY] = apiKey
        }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(GEMINI_API_KEY)
        }
    }

    suspend fun saveGeminiModel(modelId: String) {
        context.dataStore.edit { preferences ->
            preferences[GEMINI_MODEL_ID] = GeminiModels.modelIdOrDefault(modelId)
        }
    }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode.storageValue
        }
    }
}
