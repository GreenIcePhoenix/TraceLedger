package com.greenicephoenix.traceledger.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.domain.model.CategoryType
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel

@Composable
fun CategoriesScreen(
    categories: List<CategoryUiModel>,
    isLightTheme: Boolean,
    viewModel: CategoriesViewModel,        // Phase 2: added so we can observe deleteError
    onBack: () -> Unit,
    onAddCategory: () -> Unit,
    onCategoryClick: (CategoryUiModel) -> Unit
) {
    var selectedType by remember { mutableStateOf(CategoryType.EXPENSE) }

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    // Phase 2: observe delete errors reactively from the ViewModel.
    // When Room's FK constraint fires (category has linked transactions),
    // the ViewModel emits a message here and we show an AlertDialog.
    val deleteError by viewModel.deleteError.collectAsState()

    // ── DELETE BLOCKED ERROR DIALOG ───────────────────────────────────────────
    deleteError?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearDeleteError() },
            title = { Text("Cannot delete category") },
            text  = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearDeleteError() }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ── HEADER ────────────────────────────────────────────────────────────
        CategoriesHeader(
            selectedType = selectedType,
            onTypeChange = { selectedType = it },
            onBack       = onBack
        )

        // ── GRID ──────────────────────────────────────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement   = Arrangement.spacedBy(16.dp)
        ) {

            // Add card always first
            item {
                AddCategoryCard(isLightTheme = isLightTheme, onClick = onAddCategory)
            }

            // Category cards
            items(filteredCategories, key = { it.id }) { category ->
                CategoryCard(
                    category     = category,
                    isLightTheme = isLightTheme,
                    onClick      = { onCategoryClick(category) }
                )
            }

            // Bottom spacer so last row isn't hidden behind nav bar
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoriesHeader
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoriesHeader(
    selectedType: CategoryType,
    onTypeChange: (CategoryType) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint               = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text     = "CATEGORIES",
                style    = MaterialTheme.typography.headlineMedium,
                color    = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        CategoryTypeSelector(selectedType = selectedType, onTypeChange = onTypeChange)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryTypeSelector — EXPENSE / INCOME pill toggle
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryTypeSelector(
    selectedType: CategoryType,
    onTypeChange: (CategoryType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
            .padding(4.dp)
    ) {
        CategoryType.entries.forEach { type ->
            val selected = type == selectedType
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onTypeChange(type) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = type.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AddCategoryCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AddCategoryCard(isLightTheme: Boolean, onClick: () -> Unit) {
    val baseSurface     = MaterialTheme.colorScheme.surface
    val gradientColors  = if (isLightTheme) listOf(baseSurface, baseSurface)
    else listOf(Color(0xFF1A1A1A), Color(0xFF0F0F0F))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape  = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Add,
                contentDescription = "Add Category",
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(28.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryCard(
    category: CategoryUiModel,
    isLightTheme: Boolean,
    onClick: () -> Unit
) {
    val baseSurface    = MaterialTheme.colorScheme.surface
    val gradientColors = if (isLightTheme) listOf(baseSurface, baseSurface)
    else listOf(Color(0xFF1A1A1A), Color(0xFF0F0F0F))

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = CategoryIcons.all[category.icon] ?: Icons.Default.Category,
                        contentDescription = null,
                        tint               = Color(category.color),
                        modifier           = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text  = category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Colour accent line at bottom of card
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth(0.8f)
                        .background(Color(category.color).copy(alpha = 0.35f), RoundedCornerShape(1.dp))
                )
            }
        }
    }
}