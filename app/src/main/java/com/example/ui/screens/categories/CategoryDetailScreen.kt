package com.example.ui.screens.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.AppDatabase
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ScreenshotCard
import com.example.ui.viewmodel.ScreenshotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryId: String,
    viewModel: ScreenshotViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val allScreenshots by viewModel.allScreenshots.collectAsStateWithLifecycle()
    val categoryScreenshots = allScreenshots.filter { it.screenshot.categoryId == categoryId }

    val categoryMeta = AppDatabase.DEFAULT_CATEGORIES.find { it.id == categoryId }
    val titleText = if (categoryMeta != null) "${categoryMeta.icon} ${categoryMeta.name}" else "分类截图"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "$titleText (${categoryScreenshots.size})",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (categoryScreenshots.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Collections,
                title = "该分类下暂无截图",
                description = "当扫描相册或新截图归类到此类别后，将自动展示在此处。",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("category_detail_grid")
            ) {
                items(categoryScreenshots, key = { it.screenshot.id }) { item ->
                    ScreenshotCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.screenshot.id) }
                    )
                }
            }
        }
    }
}
