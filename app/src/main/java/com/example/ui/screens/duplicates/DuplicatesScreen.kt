package com.example.ui.screens.duplicates

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.similarity.SimilarGroup
import com.example.similarity.SimilarityCategory
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.DesignTokens
import com.example.ui.viewmodel.ScreenshotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DuplicateFilterTab {
    ALL, EXACT, SIMILAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    viewModel: ScreenshotViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val similarGroups by viewModel.similarGroups.collectAsStateWithLifecycle()
    val isChecking by viewModel.isCheckingDuplicates.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var singleItemToDelete by remember { mutableStateOf<com.example.data.model.ScreenshotEntity?>(null) }
    var selectedFilter by remember { mutableStateOf(DuplicateFilterTab.ALL) }

    LaunchedEffect(Unit) {
        viewModel.checkDuplicates()
    }

    val filteredGroups = remember(similarGroups, selectedFilter) {
        when (selectedFilter) {
            DuplicateFilterTab.ALL -> similarGroups
            DuplicateFilterTab.EXACT -> similarGroups.filter { it.category == SimilarityCategory.EXACT_DUPLICATE }
            DuplicateFilterTab.SIMILAR -> similarGroups.filter { it.category == SimilarityCategory.HIGH_SIMILARITY }
        }
    }

    val allRedundantIds = remember(similarGroups) {
        val set = mutableSetOf<Long>()
        for (group in similarGroups) {
            val keepId = group.recommendedKeepId
            for (item in group.items) {
                if (item.id != keepId) {
                    set.add(item.id)
                }
            }
        }
        set
    }

    val selectedTotalBytes = remember(selectedIds, similarGroups) {
        val allItems = similarGroups.flatMap { it.items }
        val idToSize = allItems.associate { it.id to it.fileSize }
        selectedIds.sumOf { idToSize[it] ?: 0L }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "重复与相似清理",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (similarGroups.isNotEmpty()) {
                            Text(
                                text = "共发现 ${similarGroups.size} 组 (${similarGroups.sumOf { it.items.size }} 张图片)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.checkDuplicates()
                            Toast.makeText(context, "正在重新检测重复与相似度...", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isChecking
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(imageVector = Icons.Outlined.Refresh, contentDescription = "刷新检测")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    tonalElevation = 0.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "已选 ${selectedIds.size} 张",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val formattedSize = Formatter.formatFileSize(context, selectedTotalBytes)
                            Text(
                                text = "预计释放 $formattedSize 空间",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { selectedIds = emptySet() },
                                shape = DesignTokens.ShapeSmall,
                                border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("取消选择", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showDeleteConfirmDialog = true },
                                shape = DesignTokens.ShapeSmall,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("批量删除", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (similarGroups.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.CleaningServices,
                title = "未发现重复或高相似截图",
                description = "相册中没有检测到完全重复或视觉高度相似的截图。若刚添加了新图片，可点击右上角重新检测。",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + (if (selectedIds.isNotEmpty()) 80.dp else 24.dp)
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("duplicates_list")
            ) {
                // Filter Tabs & Smart Select Bar
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedFilter == DuplicateFilterTab.ALL,
                                onClick = { selectedFilter = DuplicateFilterTab.ALL },
                                label = { Text("全部 (${similarGroups.size})") },
                                shape = DesignTokens.ShapeSmall,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedFilter == DuplicateFilterTab.ALL,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    borderWidth = DesignTokens.HairlineBorder
                                )
                            )
                            val exactCount = similarGroups.count { it.category == SimilarityCategory.EXACT_DUPLICATE }
                            FilterChip(
                                selected = selectedFilter == DuplicateFilterTab.EXACT,
                                onClick = { selectedFilter = DuplicateFilterTab.EXACT },
                                label = { Text("完全重复 ($exactCount)") },
                                shape = DesignTokens.ShapeSmall,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedFilter == DuplicateFilterTab.EXACT,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    borderWidth = DesignTokens.HairlineBorder
                                )
                            )
                            val similarCount = similarGroups.count { it.category == SimilarityCategory.HIGH_SIMILARITY }
                            FilterChip(
                                selected = selectedFilter == DuplicateFilterTab.SIMILAR,
                                onClick = { selectedFilter = DuplicateFilterTab.SIMILAR },
                                label = { Text("高相似度 ($similarCount)") },
                                shape = DesignTokens.ShapeSmall,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedFilter == DuplicateFilterTab.SIMILAR,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    borderWidth = DesignTokens.HairlineBorder
                                )
                            )
                        }

                        // Smart Select & Quick Actions Bar
                        Surface(
                            shape = DesignTokens.ShapeMedium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "智能清理助手",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (allRedundantIds.isNotEmpty()) "每组标记 1 张最佳保留，发现 ${allRedundantIds.size} 张建议清理" else "每组已保留最佳图片，未发现多余副本",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedIds = allRedundantIds
                                            Toast.makeText(context, "已智能勾选 ${allRedundantIds.size} 张多余截图", Toast.LENGTH_SHORT).show()
                                        },
                                        enabled = allRedundantIds.isNotEmpty(),
                                        shape = DesignTokens.ShapeSmall,
                                        border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text("智能勾选 (${allRedundantIds.size})", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            selectedIds = allRedundantIds
                                            showDeleteConfirmDialog = true
                                        },
                                        enabled = allRedundantIds.isNotEmpty(),
                                        shape = DesignTokens.ShapeSmall,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        modifier = Modifier.weight(1.2f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("清理多余项", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }

                                    val allIds = filteredGroups.flatMap { it.items }.map { it.id }.toSet()
                                    TextButton(
                                        onClick = {
                                            selectedIds = if (selectedIds.size == allIds.size) emptySet() else allIds
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            if (selectedIds.size == allIds.size && allIds.isNotEmpty()) "清空" else "全选",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Groups
                items(filteredGroups, key = { it.groupId }) { group ->
                    SimilarGroupCard(
                        group = group,
                        selectedIds = selectedIds,
                        onToggleSelect = { id ->
                            selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                        },
                        onItemClick = onNavigateToDetail,
                        onSingleDelete = { item ->
                            singleItemToDelete = item
                        }
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        val formattedSize = Formatter.formatFileSize(context, selectedTotalBytes)
        DeleteConfirmDialog(
            title = "确认删除选中的 ${selectedIds.size} 张截图？",
            message = "选中的重复或相似截图将被彻底删除并释放设备存储空间。此操作无法撤销。",
            freedSpaceText = formattedSize,
            confirmButtonText = "确认删除 (${selectedIds.size}张)",
            onConfirm = {
                val idsToDelete = selectedIds.toList()
                showDeleteConfirmDialog = false
                selectedIds = emptySet()
                viewModel.deleteScreenshots(idsToDelete) {
                    Toast.makeText(context, "已成功清理 ${idsToDelete.size} 张截图，释放空间 $formattedSize", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    if (singleItemToDelete != null) {
        val item = singleItemToDelete!!
        val formattedSize = Formatter.formatFileSize(context, item.fileSize)
        DeleteConfirmDialog(
            title = "确认删除此截图？",
            message = "此截图文件将从手机相册与截图管家中彻底删除，无法恢复。",
            freedSpaceText = formattedSize,
            confirmButtonText = "确认删除",
            onConfirm = {
                val idToDelete = item.id
                singleItemToDelete = null
                viewModel.deleteScreenshot(idToDelete) {
                    selectedIds = selectedIds - idToDelete
                    Toast.makeText(context, "已删除截图，释放空间 $formattedSize", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { singleItemToDelete = null }
        )
    }
}

@Composable
fun SimilarGroupCard(
    group: SimilarGroup,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onItemClick: (Long) -> Unit,
    onSingleDelete: (com.example.data.model.ScreenshotEntity) -> Unit
) {
    val isExact = group.category == SimilarityCategory.EXACT_DUPLICATE
    val badgeText = if (isExact) "100% 完全重复" else "${group.similarityScore}% 高度相似"

    Card(
        shape = DesignTokens.ShapeMedium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Category Badge + Reason
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = badgeText,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "共 ${group.items.size} 张",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = group.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Items in this group
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(group.items, key = { it.id }) { item ->
                    val isKeep = item.id == group.recommendedKeepId
                    val isSelected = item.id in selectedIds

                    SimilarItemCard(
                        item = item,
                        isKeep = isKeep,
                        isSelected = isSelected,
                        onToggleSelect = { onToggleSelect(item.id) },
                        onItemClick = { onItemClick(item.id) },
                        onDeleteClick = { onSingleDelete(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SimilarItemCard(
    item: com.example.data.model.ScreenshotEntity,
    isKeep: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val borderModifier = if (isSelected) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.error, DesignTokens.ShapeSmall)
    } else if (isKeep) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface, DesignTokens.ShapeSmall)
    } else {
        Modifier.border(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant, DesignTokens.ShapeSmall)
    }

    Card(
        shape = DesignTokens.ShapeSmall,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .width(140.dp)
            .then(borderModifier)
    ) {
        Column {
            // Thumbnail with overlay badges & selection checkbox
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onItemClick() }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .size(300, 360)
                        .crossfade(true)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Recommendation Badge (Top Left)
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                    color = if (isKeep) Color.Black.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = if (isKeep) "建议保留" else "多余副本",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Checkbox touch area (Top Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.error else Color.Black.copy(alpha = 0.45f))
                        .clickable { onToggleSelect() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "已选择",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Info & single delete action
            Column(modifier = Modifier.padding(8.dp)) {
                val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
                Text(
                    text = dateFormat.format(Date(item.createTime)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                val formattedSize = Formatter.formatFileSize(context, item.fileSize)
                Text(
                    text = if (item.width > 0) "${item.width}x${item.height} · $formattedSize" else formattedSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = onDeleteClick,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(DesignTokens.HairlineBorder, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("删除", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
