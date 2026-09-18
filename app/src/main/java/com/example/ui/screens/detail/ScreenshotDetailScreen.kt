package com.example.ui.screens.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.db.AppDatabase
import com.example.data.model.ScreenshotWithDetails
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.theme.DesignTokens
import com.example.ui.viewmodel.ScreenshotViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScreenshotDetailScreen(
    screenshotId: Long,
    viewModel: ScreenshotViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var itemWithDetails by remember { mutableStateOf<ScreenshotWithDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog & Dropdown States
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var editTitleInput by remember { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    // Zoom & Pan state for Image
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        if (scale == 1f) {
            offset = Offset.Zero
        } else {
            offset += offsetChange
        }
    }

    suspend fun refreshData() {
        isLoading = true
        itemWithDetails = viewModel.getScreenshotById(screenshotId)
        isLoading = false
    }

    LaunchedEffect(screenshotId) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "截图详情",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val uri = itemWithDetails?.screenshot?.uri?.let { Uri.parse(it) }
                            if (uri != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "分享截图"))
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Outlined.Share, contentDescription = "分享")
                    }

                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val uri = itemWithDetails?.screenshot?.uri?.let { Uri.parse(it) }
                            if (uri != null) {
                                Toast.makeText(context, "正在重新识别...", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    viewModel.reprocessScreenshot(screenshotId)
                                    refreshData()
                                    Toast.makeText(context, "识别完成", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = DesignTokens.ShapeSmall,
                        border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("重新识别", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = {
                            val textToCopy = itemWithDetails?.screenshot?.ocrText ?: ""
                            if (textToCopy.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Screenshot OCR", textToCopy))
                                Toast.makeText(context, "已复制全部提取文字", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "暂无提取的文字", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = DesignTokens.ShapeSmall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("复制文字", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    ) { innerPadding ->
        val currentItem = itemWithDetails
        if (currentItem == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLoading) "正在加载..." else "截图不存在或已被删除",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val screenshot = currentItem.screenshot
            val category = currentItem.category
            val tags = currentItem.tags

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Zoomable Image Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .background(Color(0xFF121212))
                        .transformable(state = transformState),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(screenshot.uri)
                            .crossfade(false)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .precision(coil.size.Precision.INEXACT)
                            .build(),
                        contentDescription = screenshot.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                }

                // 2. Main Info Card
                Column(modifier = Modifier.padding(16.dp)) {
                    // Title & Category Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    editTitleInput = screenshot.title
                                    showEditTitleDialog = true
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val title = if (screenshot.title.isNotBlank()) screenshot.title else screenshot.fileName
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "修改标题",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Category Dropdown Button
                        Box {
                            Surface(
                                shape = DesignTokens.ShapeSmall,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clip(DesignTokens.ShapeSmall)
                                    .clickable { showCategoryDropdown = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val catName = category?.name ?: "未分类"
                                    Text(
                                        text = catName,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                            ) {
                                AppDatabase.DEFAULT_CATEGORIES.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            viewModel.updateCategory(screenshot.id, cat.id)
                                            itemWithDetails = itemWithDetails?.copy(
                                                category = cat,
                                                screenshot = screenshot.copy(categoryId = cat.id)
                                            )
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // AI Summary Banner if available
                    if (screenshot.summary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = DesignTokens.ShapeCard,
                            border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = screenshot.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Tags FlowRow
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "标签",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text("#${tag.name}") },
                                shape = DesignTokens.ShapeSmall,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val remaining = tags.filter { it.id != tag.id }.map { it.name }
                                            viewModel.updateTags(screenshot.id, remaining)
                                            itemWithDetails = itemWithDetails?.copy(
                                                tags = tags.filter { it.id != tag.id }
                                            )
                                        },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "删除标签",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            )
                        }

                        SuggestionChip(
                            onClick = { showAddTagDialog = true },
                            label = { Text("+ 添加标签") },
                            shape = DesignTokens.ShapeSmall
                        )
                    }

                    // OCR Text Section
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "提取的文字内容",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                val text = screenshot.ocrText
                                if (text.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Screenshot OCR", text))
                                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "复制文字",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = DesignTokens.ShapeCard,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = if (screenshot.ocrText.isNotBlank()) screenshot.ocrText else "未识别到文字内容",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    // Metadata Section
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "图片参数",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = DesignTokens.ShapeCard,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            MetadataRow(label = "保存时间", value = dateFormat.format(Date(screenshot.createTime)))
                            MetadataRow(label = "文件名", value = screenshot.fileName)
                            if (screenshot.width > 0 && screenshot.height > 0) {
                                MetadataRow(label = "分辨率", value = "${screenshot.width} × ${screenshot.height}")
                            }
                            if (screenshot.fileSize > 0) {
                                val sizeInMb = String.format(Locale.getDefault(), "%.2f MB", screenshot.fileSize / (1024f * 1024f))
                                MetadataRow(label = "大小", value = sizeInMb)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        val sizeText = itemWithDetails?.screenshot?.fileSize?.let { Formatter.formatFileSize(context, it) }
        DeleteConfirmDialog(
            title = "确认删除此截图？",
            message = "截图文件将从手机相册与截图管家中彻底删除，无法恢复。",
            freedSpaceText = sizeText,
            confirmButtonText = "确认彻底删除",
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteScreenshot(screenshotId) {
                    Toast.makeText(context, "已删除截图", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    // Add Tag Dialog
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = {
                Text(
                    text = "添加标签",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("输入标签名称，如 显卡 / 账单") },
                    shape = DesignTokens.ShapeSmall,
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newTagInput.trim().removePrefix("#")
                        if (trimmed.isNotBlank()) {
                            val currentTags = itemWithDetails?.tags?.map { it.name } ?: emptyList()
                            val updatedList = (currentTags + trimmed).distinct()
                            viewModel.updateTags(screenshotId, updatedList)
                            newTagInput = ""
                            showAddTagDialog = false
                            coroutineScope.launch { refreshData() }
                        }
                    }
                ) {
                    Text("保存", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = DesignTokens.ShapeCard,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Edit Title Dialog
    if (showEditTitleDialog) {
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            title = {
                Text(
                    text = "修改标题",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                OutlinedTextField(
                    value = editTitleInput,
                    onValueChange = { editTitleInput = it },
                    placeholder = { Text("输入新标题") },
                    shape = DesignTokens.ShapeSmall,
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editTitleInput.isNotBlank()) {
                            viewModel.updateTitle(screenshotId, editTitleInput.trim())
                            itemWithDetails = itemWithDetails?.let {
                                it.copy(screenshot = it.screenshot.copy(title = editTitleInput.trim()))
                            }
                            showEditTitleDialog = false
                        }
                    }
                ) {
                    Text("保存", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = DesignTokens.ShapeCard,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
