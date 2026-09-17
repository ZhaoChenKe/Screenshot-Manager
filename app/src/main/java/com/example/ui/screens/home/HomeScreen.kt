package com.example.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProgressBarCard
import com.example.ui.components.ScreenshotCard
import com.example.ui.components.StatCard
import com.example.ui.components.UpdateDialog
import com.example.ui.util.CategoryUiHelper
import com.example.ui.viewmodel.ScreenshotViewModel
import com.example.update.UpdateCheckResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ScreenshotViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDuplicates: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCategory: (String) -> Unit
) {
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val unprocessedCount by viewModel.unprocessedCount.collectAsStateWithLifecycle()
    val recentScreenshots by viewModel.recentScreenshots.collectAsStateWithLifecycle()
    val progress by viewModel.processingProgress.collectAsStateWithLifecycle()
    val categoriesWithCount by viewModel.categoriesWithCount.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val similarGroups by viewModel.similarGroups.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val updateCheckResult by viewModel.updateCheckResult.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var isBatchMode by remember { mutableStateOf(false) }
    var selectedBatchIds by remember { mutableStateOf(setOf<Long>()) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    val filteredScreenshots = remember(recentScreenshots, selectedCategoryId) {
        if (selectedCategoryId == null) {
            recentScreenshots
        } else {
            recentScreenshots.filter { it.screenshot.categoryId.equals(selectedCategoryId, ignoreCase = true) }
        }
    }

    val context = LocalContext.current
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        hasStoragePermission = granted
        if (granted) {
            viewModel.startScan()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasStoragePermission) {
            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionLauncher.launch(permissionsToRequest)
        } else {
            viewModel.startScan()
        }
    }

    Scaffold(
        topBar = {
            if (isBatchMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "已选择 ${selectedBatchIds.size} 项",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isBatchMode = false
                            selectedBatchIds = emptySet()
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "退出批量管理")
                        }
                    },
                    actions = {
                        val allFilteredIds = filteredScreenshots.map { it.screenshot.id }.toSet()
                        TextButton(onClick = {
                            selectedBatchIds = if (selectedBatchIds.size == allFilteredIds.size && allFilteredIds.isNotEmpty()) {
                                emptySet()
                            } else {
                                allFilteredIds
                            }
                        }) {
                            Text(
                                text = if (selectedBatchIds.size == allFilteredIds.size && allFilteredIds.isNotEmpty()) "清空" else "全选",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(9.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_app_brand_logo_tintable),
                                    contentDescription = "Logo",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "截图管家",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "离线OCR",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.loadSampleScreenshots() },
                            modifier = Modifier.testTag("home_load_samples_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "导入演示示例截图",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.startScan() },
                            modifier = Modifier.testTag("home_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "扫描相册截图"
                            )
                        }
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "设置"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isBatchMode && selectedBatchIds.isNotEmpty(),
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
                        Text(
                            text = "已选 ${selectedBatchIds.size} 张截图",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    isBatchMode = false
                                    selectedBatchIds = emptySet()
                                }
                            ) {
                                Text("完成")
                            }

                            Button(
                                onClick = { showBatchDeleteDialog = true },
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screenshot_grid")
        ) {
            // 1. Search Bar Trigger
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onNavigateToSearch)
                        .testTag("home_search_bar_trigger"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "搜索截图文字、价格、商品、单号...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "AI·OCR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // 2. Permission Banner if needed
            if (!hasStoragePermission && totalCount == 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "需要相册读取权限",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "为了自动扫描手机中的截图并提取文字，请授予相册访问权限。所有数据优先在本地离线处理。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                                    } else {
                                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                    permissionLauncher.launch(perms)
                                }
                            ) {
                                Text("立即授权")
                            }
                        }
                    }
                }
            }

            // 3. Progress Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                ProgressBarCard(progress = progress)
            }

            // 4. Stat Cards (All, Unprocessed, Duplicates)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "全部截图",
                        count = totalCount,
                        icon = Icons.Default.Collections,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "待整理",
                        count = unprocessedCount,
                        icon = Icons.Default.PendingActions,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = if (unprocessedCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                        onClick = { viewModel.startProcessPending() },
                        modifier = Modifier.weight(1f)
                    )
                    val dupCount = if (similarGroups.isNotEmpty()) similarGroups.size else duplicateGroups.size
                    StatCard(
                        title = "重复/相似",
                        count = dupCount,
                        icon = Icons.Default.CleaningServices,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = if (dupCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                        onClick = onNavigateToDuplicates,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Redesigned Category Filter Bar
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "图片分类",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                val activeCategoryCount = categoriesWithCount.count { it.count > 0 }
                                Text(
                                    text = "$activeCategoryCount 个包含截图",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "全部分类 ›",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (selectedCategoryId != null) {
                                        onNavigateToCategory(selectedCategoryId!!)
                                    } else {
                                        onNavigateToCategory("shopping")
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        // "全部" (All) capsule
                        item {
                            val isAllSelected = selectedCategoryId == null
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isAllSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
                                border = if (isAllSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedCategoryId = null }
                                    .testTag("category_filter_all")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.GridView,
                                        contentDescription = "全部",
                                        tint = if (isAllSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "全部",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAllSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isAllSelected) MaterialTheme.colorScheme.surface.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "$totalCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAllSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Category items
                        items(categoriesWithCount, key = { it.id }) { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            val style = CategoryUiHelper.getStyle(cat.id)

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) style.accentColor else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedCategoryId = if (isSelected) null else cat.id
                                    }
                                    .testTag("category_filter_${cat.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = style.icon,
                                        contentDescription = cat.name,
                                        tint = if (isSelected) Color.White else style.accentColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (cat.count > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else style.lightBgColor
                                        ) {
                                            Text(
                                                text = "${cat.count}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else style.accentColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Recent Screenshots Section Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sectionTitle = if (selectedCategoryId != null) {
                        "${CategoryUiHelper.getStyle(selectedCategoryId!!).displayName} 截图"
                    } else {
                        "最近截图"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sectionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "共 ${filteredScreenshots.size} 张",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedCategoryId != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedCategoryId = null }
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "清除筛选",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "清除筛选",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (filteredScreenshots.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isBatchMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        isBatchMode = !isBatchMode
                                        if (!isBatchMode) selectedBatchIds = emptySet()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Checklist,
                                        contentDescription = null,
                                        tint = if (isBatchMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBatchMode) "完成" else "批量管理",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isBatchMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. Grid items
            if (filteredScreenshots.isEmpty() && !progress.isScanning) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    if (selectedCategoryId != null) {
                        val style = CategoryUiHelper.getStyle(selectedCategoryId!!)
                        EmptyStateView(
                            icon = style.icon,
                            title = "暂无${style.displayName}分类截图",
                            description = "当前分类下暂无已识别截图。您可以点击下方按钮返回查看全部截图，或导入演示截图。",
                            actionButtonText = "查看全部截图",
                            onActionClick = { selectedCategoryId = null },
                            secondaryButtonText = "导入示例截图",
                            onSecondaryClick = { viewModel.loadSampleScreenshots() },
                            modifier = Modifier.padding(top = 28.dp)
                        )
                    } else {
                        EmptyStateView(
                            icon = Icons.Default.Collections,
                            title = "暂无截图",
                            description = "相册中暂未检测到截图。您可以一键导入演示示例截图体验全部功能，或点击扫描设备相册。",
                            actionButtonText = "导入演示示例截图",
                            onActionClick = { viewModel.loadSampleScreenshots() },
                            secondaryButtonText = "扫描设备相册",
                            onSecondaryClick = { viewModel.startScan() },
                            modifier = Modifier.padding(top = 28.dp)
                        )
                    }
                }
            } else {
                items(filteredScreenshots, key = { it.screenshot.id }) { item ->
                    ScreenshotCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.screenshot.id) },
                        isSelectionMode = isBatchMode,
                        isSelected = item.screenshot.id in selectedBatchIds,
                        onToggleSelect = {
                            val id = item.screenshot.id
                            selectedBatchIds = if (id in selectedBatchIds) selectedBatchIds - id else selectedBatchIds + id
                        }
                    )
                }
            }
        }
    }

    if (showBatchDeleteDialog) {
        val totalBytes = recentScreenshots.filter { it.screenshot.id in selectedBatchIds }.sumOf { it.screenshot.fileSize }
        val formattedSize = Formatter.formatFileSize(context, totalBytes)
        DeleteConfirmDialog(
            title = "确认批量删除 ${selectedBatchIds.size} 张截图？",
            message = "所选截图将从设备相册中彻底移除并释放存储空间。此操作无法撤销。",
            freedSpaceText = formattedSize,
            confirmButtonText = "确认删除 (${selectedBatchIds.size}张)",
            onConfirm = {
                val idsToDelete = selectedBatchIds.toList()
                showBatchDeleteDialog = false
                viewModel.deleteScreenshots(idsToDelete) {
                    selectedBatchIds = emptySet()
                    isBatchMode = false
                    Toast.makeText(context, "已成功删除 ${idsToDelete.size} 张截图，释放空间 $formattedSize", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showBatchDeleteDialog = false }
        )
    }

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
