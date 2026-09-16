package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ScreenshotViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScreenshotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming Android Share intents (e.g. from WeChat, Taobao, Browser)
        handleShareIntent(intent)

        setContent {
            val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
            val isDark = when (userPreferences.themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                AppNavigation(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type?.startsWith("image/") == true) {
            val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }

            if (imageUri != null) {
                viewModel.importFromUri(imageUri, "Shared_Image_${System.currentTimeMillis()}.jpg") {
                    Toast.makeText(this, "已导入截图并开始识别", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action && type?.startsWith("image/") == true) {
            val imageUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            }

            if (!imageUris.isNullOrEmpty()) {
                var count = 0
                for (uri in imageUris) {
                    viewModel.importFromUri(uri, "Shared_Image_${System.currentTimeMillis()}_$count.jpg")
                    count++
                }
                Toast.makeText(this, "已导入 $count 张截图并开始识别", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
