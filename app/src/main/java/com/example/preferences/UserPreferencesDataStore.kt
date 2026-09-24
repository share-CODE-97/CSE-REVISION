package com.example.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

enum class AppThemeMode {
    DARK, LIGHT, SYSTEM
}

class UserPreferencesDataStore(private val context: Context) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("app_theme_mode")
        private val KEY_AI_MODEL = stringPreferencesKey("selected_ai_model")

        const val DEFAULT_MODEL = "gemini-3.5-flash"

        val AVAILABLE_MODELS = listOf(
            "gemini-3.5-flash",
            "gemini-3.1-pro-preview",
            "gemini-3.1-flash-lite-preview"
        )
    }

    // Default theme is DARK per requirement 67
    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[KEY_THEME]) {
            AppThemeMode.LIGHT.name -> AppThemeMode.LIGHT
            AppThemeMode.SYSTEM.name -> AppThemeMode.SYSTEM
            else -> AppThemeMode.DARK
        }
    }

    val selectedModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_AI_MODEL] ?: DEFAULT_MODEL
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = mode.name
        }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AI_MODEL] = model
        }
    }
}
