package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vibechat_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_PROVIDER = "provider"
        const val KEY_GEMINI_KEY = "gemini_key"
        const val KEY_GEMINI_MODEL = "gemini_model"
        const val KEY_OPENAI_KEY = "openai_key"
        const val KEY_OPENAI_MODEL = "openai_model"
        const val KEY_OPENAI_URL = "openai_url"
        const val KEY_DEEPSEEK_KEY = "deepseek_key"
        const val KEY_DEEPSEEK_MODEL = "deepseek_model"
        const val KEY_DEEPSEEK_URL = "deepseek_url"
        const val KEY_CUSTOM_KEY = "custom_key"
        const val KEY_CUSTOM_MODEL = "custom_model"
        const val KEY_CUSTOM_URL = "custom_url"

        // Mode toggles
        const val KEY_TEMPERATURE = "temperature"
    }

    var provider: String
        get() = prefs.getString(KEY_PROVIDER, "Gemini") ?: "Gemini"
        set(value) = prefs.edit().putString(KEY_PROVIDER, value).apply()

    var geminiKey: String
        get() = prefs.getString(KEY_GEMINI_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_KEY, value).apply()

    var geminiModel: String
        get() = prefs.getString(KEY_GEMINI_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash"
        set(value) = prefs.edit().putString(KEY_GEMINI_MODEL, value).apply()

    var openAIKey: String
        get() = prefs.getString(KEY_OPENAI_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENAI_KEY, value).apply()

    var openAIModel: String
        get() = prefs.getString(KEY_OPENAI_MODEL, "gpt-4o-mini") ?: "gpt-4o-mini"
        set(value) = prefs.edit().putString(KEY_OPENAI_MODEL, value).apply()

    var openAIUrl: String
        get() = prefs.getString(KEY_OPENAI_URL, "https://api.openai.com/v1/") ?: "https://api.openai.com/v1/"
        set(value) = prefs.edit().putString(KEY_OPENAI_URL, value).apply()

    var deepseekKey: String
        get() = prefs.getString(KEY_DEEPSEEK_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEEPSEEK_KEY, value).apply()

    var deepseekModel: String
        get() = prefs.getString(KEY_DEEPSEEK_MODEL, "deepseek-chat") ?: "deepseek-chat"
        set(value) = prefs.edit().putString(KEY_DEEPSEEK_MODEL, value).apply()

    var deepseekUrl: String
        get() = prefs.getString(KEY_DEEPSEEK_URL, "https://api.deepseek.com/v1/") ?: "https://api.deepseek.com/v1/"
        set(value) = prefs.edit().putString(KEY_DEEPSEEK_URL, value).apply()

    var customKey: String
        get() = prefs.getString(KEY_CUSTOM_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_KEY, value).apply()

    var customModel: String
        get() = prefs.getString(KEY_CUSTOM_MODEL, "custom-model") ?: "custom-model"
        set(value) = prefs.edit().putString(KEY_CUSTOM_MODEL, value).apply()

    var customUrl: String
        get() = prefs.getString(KEY_CUSTOM_URL, "https://api.example.com/v1/") ?: "https://api.example.com/v1/"
        set(value) = prefs.edit().putString(KEY_CUSTOM_URL, value).apply()

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()

    fun getActiveUrlAndKeyAndModel(buildConfigGeminiKey: String): Triple<String, String, String> {
        return when (provider) {
            "Gemini" -> {
                val key = geminiKey.ifEmpty { buildConfigGeminiKey }
                Triple("https://generativelanguage.googleapis.com/", key, geminiModel)
            }
            "OpenAI" -> Triple(openAIUrl, openAIKey, openAIModel)
            "DeepSeek" -> Triple(deepseekUrl, deepseekKey, deepseekModel)
            else -> Triple(customUrl, customKey, customModel)
        }
    }
}
