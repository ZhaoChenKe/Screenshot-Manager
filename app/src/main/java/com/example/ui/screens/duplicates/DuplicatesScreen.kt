package com.example.ui.screens.duplicates

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.ScreenshotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DuplicateFilterTab {
    ALL,
    EXACT,
    SIMILAR
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

    var selectedFilter by remember { mutableStateOf(DuplicateFilterTab.ALL) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val filteredGroups = remember(similarGroups, selectedFilter) {
        when (selectedFilter) {
            DuplicateFilterTab.ALL -> similarGroups
            DuplicateFilterTab.EXACT -> similarGroups.filter { it.category == SimilarityCategory.EXACT_DUPLICATE }
            DuplicateFilterTab.SIMILAR -> similarGroups.filter { it.category == SimilarityCategory.HIGH_SIMILARITY }
        }
    }

    // Calculate total cleanable redundant items (all items except recommendedKeep in each group)
    val allRedundantIds = remember(filteredGroups) {
        filteredGroups.flatMap { group ->
            group.items.filter { it.id != group.recommendedKeepId }.map { it.id }
        }.toSet()
    }

    // Calculate selected bytes
    val selectedTotalBytes = remember(selectedIds, similarGroups) {
        similarGroups.flatMap { it.items }
            .filter { it.id in selectedIds }
            .distinctBy { it.id }
            .sumOf { it.fileSize }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "检查重复与相似图片",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
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
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "刷新检测")
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
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "已选 ${selectedIds.size} 张",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val formattedSize = Formatter.formatFileSize(context, selectedTotalBytes)
                            Text(
                                text = "预计释放 $formattedSize 空间",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { selectedIds = emptySet() }
                            ) {
                                Text("取消选择")
                            }

                            Button(
                                onClick = { showDeleteConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("批量删除")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (similarGroups.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.CleaningServices,
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
                                label = { Text("全部 (${similarGroups.size})") }
                            )
                            val exactCount = similarGroups.count { it.category == SimilarityCategory.EXACT_DUPLICATE }
                            FilterChip(
                                selected = selectedFilter == DuplicateFilterTab.EXACT,
                                onClick = { selectedFilter = DuplicateFilterTab.EXACT },
                                label = { Text("完全重复 ($exactCount)") }
                            )
                            val similarCount = similarGroups.count { it.category == SimilarityCategory.HIGH_SIMILARITY }
                            FilterChip(
                                selected = selectedFilter == DuplicateFilterTab.SIMILAR,
                                onClick = { selectedFilter = DuplicateFilterTab.SIMILAR },
                                label = { Text("高相似度 ($similarCount)") }
                            )
                        }

                        // Smart Select & Quick Actions Bar
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "智能清理助手",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            // Select all redundant copies (leaving recommended keep item per group)
                                            selectedIds = allRedundantIds
                                            Toast.makeText(context, "已为您智能勾选每组建议清理项", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("智能推荐勾选", fontSize = 13.sp)
                                    }

                                    TextButton(
                                        onClick = {
                                            val allIds = filteredGroups.flatMap { it.items }.map { it.id }.toSet()
                                            selectedIds = if (selectedIds.size == allIds.size) emptySet() else allIds
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        val allIds = filteredGroups.flatMap { it.items }.map { it.id }.toSet()
                                        Text(
                                            if (selectedIds.size == allIds.size && allIds.isNotEmpty()) "清空" else "全选",
                                            fontSize = 13.sp
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
                        onSingleDelete = { id ->
                            viewModel.deleteScreenshot(id) {
                                selectedIds = selectedIds - id
                                Toast.makeText(context, "已删除截图", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        val formattedSize = Formatter.formatFileSize(context, selectedTotalBytes)
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认批量删除", fontWeight = FontWeight.Bold) },
            text = {
                Text("确定要删除选中的 ${selectedIds.size} 张截图吗？将释放约 $formattedSize 存储空间。删除后无法恢复。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToDelete = selectedIds.toList()
                        showDeleteConfirmDialog = false
                        viewModel.deleteScreenshots(idsToDelete) {
                            selectedIds = emptySet()
                            Toast.makeText(context, "已成功批量删除 ${idsToDelete.size} 张截图", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun SimilarGroupCard(
    group: SimilarGroup,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onItemClick: (Long) -> Unit,
    onSingleDelete: (Long) -> Unit
) {
    val isExact = group.category == SimilarityCategory.EXACT_DUPLICATE
    val badgeBg = if (isExact) Color(0xFFFFEDD5) else Color(0xFFEDE9FE)
    val badgeColor = if (isExact) Color(0xFFEA580C) else Color(0xFF7C3AED)
    val badgeText = if (isExact) "100% 完全重复" else "${group.similarityScore}% 高度相似"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
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
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "共 ${group.items.size} 张",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = group.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                        onDeleteClick = { onSingleDelete(item.id) }
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
        Modifier.border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
    } else if (isKeep) {
        Modifier.border(1.5.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .width(150.dp)
            .then(borderModifier)
    ) {
        Column {
            // Thumbnail with overlay badges & selection checkbox
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clickable { onItemClick() }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Recommendation Badge (Top Left)
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    color = if (isKeep) Color(0xFF10B981) else Color(0xCC000000),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = if (isKeep) "建议保留" else "冗余项",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Checkbox touch area (Top Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.error else Color.Black.copy(alpha = 0.5f))
                        .clickable { onToggleSelect() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已选择",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
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
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
