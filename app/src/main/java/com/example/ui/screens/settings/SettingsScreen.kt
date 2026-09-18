package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.ui.components.UpdateDialog
import com.example.ui.theme.DesignTokens
import com.example.ui.viewmodel.ScreenshotViewModel
import com.example.update.DownloadState
import com.example.update.UpdateCheckResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScreenshotViewModel,
    onNavigateBack: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val updateCheckResult by viewModel.updateCheckResult.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(prefs.customApiKey) }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showCustomUrlDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf(prefs.customUpdateUrl) }

    // Toast feedback for manual update check results
    LaunchedEffect(updateCheckResult) {
        when (updateCheckResult) {
            is UpdateCheckResult.UpToDate -> {
                Toast.makeText(context, "当前已是最新版本 (v${(updateCheckResult as UpdateCheckResult.UpToDate).currentVersionName})", Toast.LENGTH_SHORT).show()
            }
            is UpdateCheckResult.Error -> {
                Toast.makeText(context, (updateCheckResult as UpdateCheckResult.Error).message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    val lastCheckText = remember(prefs.lastUpdateCheckTime) {
        if (prefs.lastUpdateCheckTime <= 0L) {
            "从未检查"
        } else {
            val diff = System.currentTimeMillis() - prefs.lastUpdateCheckTime
            val minutes = diff / (60 * 1000)
            val hours = diff / (60 * 60 * 1000)
            val days = diff / (24 * 60 * 60 * 1000)
            when {
                minutes < 1 -> "刚刚"
                minutes < 60 -> "${minutes}分钟前"
                hours < 24 -> "${hours}小时前"
                else -> "${days}天前"
            }
        }
    }

    val frequencyText = when (prefs.autoCheckUpdateFrequencyDays) {
        1 -> "每天检查一次"
        7 -> "每周检查一次 (默认)"
        14 -> "每两周检查一次"
        30 -> "每月检查一次"
        0 -> "从不自动检查"
        else -> "每${prefs.autoCheckUpdateFrequencyDays}天检查一次"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "设置与偏好", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Scanning Settings Card
            Text(text = "扫描与整理", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Card(
                shape = DesignTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "自动扫描新截图", fontWeight = FontWeight.Medium)
                            Text(
                                text = "启动应用时自动检索手机相册中新生成的截图",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.autoScanNew,
                            onCheckedChange = { viewModel.setAutoScanNew(it) }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DesignTokens.ShapeSmall)
                            .clickable {
                                viewModel.startScan()
                                Toast.makeText(context, "已启动全面扫描相册", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "立即重新扫描相册", fontWeight = FontWeight.Medium)
                            Text(
                                text = "重新同步设备所有截图目录",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. AI Understanding Settings Card
            Text(text = "AI 智能增强", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Card(
                shape = DesignTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "启用 AI 语义分析", fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "利用 Gemini 提取结构化商品、会议要点或智能标签",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.enableAiAnalysis,
                            onCheckedChange = { viewModel.setEnableAiAnalysis(it) }
                        )
                    }

                    // Privacy Note
                    Surface(
                        shape = DesignTokens.ShapeSmall,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(imageVector = Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "隐私说明：开启 AI 增强时会将截图提取文字发往 AI 服务进行理解。关闭状态下所有 OCR、分类、检索全部在本地设备离线进行。",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DesignTokens.ShapeSmall)
                            .clickable {
                                apiKeyInput = prefs.customApiKey
                                showApiKeyDialog = true
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "配置自定义 API Key", fontWeight = FontWeight.Medium)
                            val keyStatus = if (prefs.customApiKey.isNotBlank()) "已配置自定义 Key" else "使用系统默认 Key"
                            Text(
                                text = keyStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Theme Card
            Text(text = "显示与外观", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Card(
                shape = DesignTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DesignTokens.ShapeCard)
                        .clickable { showThemeDialog = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "深色模式", fontWeight = FontWeight.Medium)
                        val themeText = when (prefs.themeMode) {
                            "light" -> "浅色"
                            "dark" -> "深色"
                            else -> "跟随系统"
                        }
                        Text(text = themeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 4. Rescan Device Album Card
            Text(text = "相册同步", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Card(
                shape = DesignTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DesignTokens.ShapeSmall)
                            .clickable {
                                viewModel.startScan()
                                Toast.makeText(context, "正在重新扫描设备相册...", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "重新扫描设备相册", fontWeight = FontWeight.Medium)
                            Text(
                                text = "从手机媒体库中扫描全部截屏并增量同步",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 5. Version & Update & About
            Text(text = "版本与关于", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Card(
                shape = DesignTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // App Brand & Version info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(DesignTokens.ShapeSmall)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant, DesignTokens.ShapeSmall),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_app_brand_logo_tintable),
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "截图管家", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "独立分发版",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "当前版本 v${BuildConfig.VERSION_NAME} (构建 ${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 1) Manual Check Update button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DesignTokens.ShapeSmall)
                            .clickable {
                                viewModel.checkForUpdate(isManual = true, simulateIfNoUrl = true)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (updateCheckResult is UpdateCheckResult.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "检查版本更新", fontWeight = FontWeight.Medium)
                            Text(
                                text = "上次检查：$lastCheckText · 点击直接从手机端检测更新",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 2) Auto Check Frequency (Default: 1 week / 7 days)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DesignTokens.ShapeSmall)
                            .clickable {
                                showFrequencyDialog = true
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "自动检查更新周期", fontWeight = FontWeight.Medium)
                            Text(
                                text = frequencyText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 3) Custom Update Source URL (Optional)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DesignTokens.ShapeSmall)
                            .clickable {
                                customUrlInput = prefs.customUpdateUrl
                                showCustomUrlDialog = true
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "版本更新服务源", fontWeight = FontWeight.Medium)
                            Text(
                                text = if (prefs.customUpdateUrl.isBlank()) "默认官方源：Gitee Releases (国内极速直连)" else prefs.customUpdateUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "无需应用商店 · 手机端直连下载 APK · 本地安全沙箱与 FileProvider 静默安装",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("配置 Gemini API Key", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text(
                        text = "如果您有自己的 Google AI Gemini API Key，可在此填入。留空则默认使用应用预置配置。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        shape = DesignTokens.ShapeSmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setCustomApiKey(apiKeyInput.trim())
                        showApiKeyDialog = false
                        Toast.makeText(context, "已保存 API Key", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("保存", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = DesignTokens.ShapeCard,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    listOf("system" to "跟随系统", "light" to "浅色模式", "dark" to "深色模式").forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(key)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = prefs.themeMode == key,
                                onClick = {
                                    viewModel.setThemeMode(key)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("关闭", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            shape = DesignTokens.ShapeCard,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Auto Check Frequency Dialog
    if (showFrequencyDialog) {
        AlertDialog(
            onDismissRequest = { showFrequencyDialog = false },
            title = { Text("自动检查更新周期", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    listOf(
                        7 to "每周检查一次 (默认推荐)",
                        1 to "每天检查一次",
                        14 to "每两周检查一次",
                        30 to "每月检查一次",
                        0 to "从不自动检查 (仅支持手动检查)"
                    ).forEach { (days, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAutoCheckUpdateFrequencyDays(days)
                                    showFrequencyDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = prefs.autoCheckUpdateFrequencyDays == days,
                                onClick = {
                                    viewModel.setAutoCheckUpdateFrequencyDays(days)
                                    showFrequencyDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFrequencyDialog = false }) {
                    Text("关闭", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            shape = DesignTokens.ShapeCard,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Custom Update URL Dialog
    if (showCustomUrlDialog) {
        AlertDialog(
            onDismissRequest = { showCustomUrlDialog = false },
            title = { Text("配置版本更新服务源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text(
                        text = "默认通过 Gitee 官方仓库 (zhao-chenke/Screenshot-Manager) Releases 检测最新版本并国内极速下载 APK。亦支持切换为 GitHub Releases 或自建服务器 version.json 链接。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        placeholder = { Text("https://gitee.com/api/v5/repos/zhao-chenke/Screenshot-Manager/releases/latest") },
                        singleLine = true,
                        shape = DesignTokens.ShapeSmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setCustomUpdateUrl(customUrlInput.trim())
                        showCustomUrlDialog = false
                        Toast.makeText(context, "已保存自定义更新源", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("保存", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            dismissButton = {
                Row {
                    if (prefs.customUpdateUrl.isNotBlank()) {
                        TextButton(
                            onClick = {
                                customUrlInput = ""
                                viewModel.setCustomUpdateUrl("")
                                showCustomUrlDialog = false
                                Toast.makeText(context, "已恢复默认更新源", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("恢复默认", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = { showCustomUrlDialog = false }) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            shape = DesignTokens.ShapeCard,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // App Update Dialog (has update found)
    if (showUpdateDialog && updateCheckResult is UpdateCheckResult.HasUpdate) {
        val updateInfo = (updateCheckResult as UpdateCheckResult.HasUpdate).updateInfo
        UpdateDialog(
            updateInfo = updateInfo,
            downloadState = downloadState,
            onStartDownload = {
                viewModel.startDownloadAndInstall(updateInfo)
            },
            onDismiss = {
                viewModel.dismissUpdateDialog()
            }
        )
    }
}
