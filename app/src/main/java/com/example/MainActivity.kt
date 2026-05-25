package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.SettingsRepository
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SQLite Database and parameters
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "vibechat_db")
            .fallbackToDestructiveMigration()
            .build()

        val settingsRepository = SettingsRepository(applicationContext)

        // Securely fetch Gemini API token injected from AI Studio secrets
        val developerSecretsApiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val viewModel = ChatViewModel(
            messageDao = db.messageDao(),
            settingsRepository = settingsRepository,
            buildConfigGeminiKey = developerSecretsApiKey
        )

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "chat") {
                    composable("chat") {
                        ChatScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            settingsRepository = settingsRepository,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
