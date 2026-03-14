package com.greenicephoenix.traceledger.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.domain.model.CategoryType
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.luminance


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryScreen(
    existingCategory: CategoryUiModel? = null,
    onCancel: () -> Unit,
    onSave: (CategoryUiModel) -> Unit
) {
    val isEditMode = existingCategory != null

    var name by remember {
        mutableStateOf(existingCategory?.name ?: "")
    }

    var type by remember {
        mutableStateOf(existingCategory?.type ?: CategoryType.EXPENSE)
    }

    val initialColor = remember(existingCategory?.id) {
        if (existingCategory != null) {
            Color(existingCategory.color)
        } else {
            CategoryColors.expenseColors.first()
        }
    }

    var selectedColor by remember(existingCategory?.id) {
        mutableStateOf(initialColor)
    }


    var selectedIcon by remember {
        mutableStateOf(existingCategory?.icon ?: "category")
    }

    val isValid by remember {
        derivedStateOf { name.isNotBlank() }
    }

    val isLightTheme =
        MaterialTheme.colorScheme.background.luminance() > 0.5f

    val cardGradientColors =
        if (isLightTheme) {
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface
            )
        } else {
            listOf(
                Color(0xFF1A1A1A),
                Color(0xFF0F0F0F)
            )
        }


    Scaffold { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
            //.padding(padding)
        ) {

            // ================= HEADER =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = if (isEditMode) "Edit Category" else "Add Category",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(
                    onClick = {
                        if (!isValid) return@IconButton

                        val category = CategoryUiModel(
                            id = existingCategory?.id
                                ?: java.util.UUID.randomUUID().toString(),
                            name = name.trim(),
                            type = type,
                            color = selectedColor.toArgb().toLong(),
                            icon = selectedIcon
                        )

                        onSave(category)
                    },
                    enabled = isValid
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = if (isValid) NothingRed else Color.Gray
                    )
                }
            }

            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
            )

            // ================= CONTENT =================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.verticalGradient(cardGradientColors),
                                shape = RoundedCornerShape(28.dp)
                            )
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {

                            item {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Category Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            item {
                                CategoryTypeSelector(
                                    selected = type,
                                    enabled = !isEditMode,
                                    onSelected = { type = it }
                                )
                            }

                            item {
                                SectionLabel("ICON")
                            }

                            item {
                                IconPicker(
                                    selectedIcon = selectedIcon,
                                    onIconSelect = { selectedIcon = it }
                                )
                            }

                            item {
                                SectionLabel("COLOR")
                            }

                            item {
                                ColorPicker(
                                    colors = if (type == CategoryType.EXPENSE)
                                        CategoryColors.expenseColors
                                    else
                                        CategoryColors.incomeColors,
                                    selectedColor = selectedColor,
                                    onColorSelect = { selectedColor = it }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTypeSelector(
    selected: CategoryType,
    enabled: Boolean,
    onSelected: (CategoryType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(22.dp)
            )
            .padding(4.dp)
    ) {
        CategoryType.entries.forEach { type ->
            val isSelected = type == selected

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isSelected) NothingRed.copy(alpha = 0.25f)
                        else Color.Transparent,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable(enabled) { onSelected(type) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type.name,
                    color = if (isSelected) NothingRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IconPicker(
    selectedIcon: String,
    onIconSelect: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 44.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(CategoryIcons.ids) { iconId ->
            val selected = iconId == selectedIcon

            Box(
                modifier = Modifier
                    .size(44.dp) // ✅ square touch target
                    .clickable { onIconSelect(iconId) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp) // ✅ visual circle
                        .clip(CircleShape)
                        .background(
                            if (selected)
                                NothingRed.copy(alpha = 0.25f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryIcons.all[iconId]!!,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorPicker(
    colors: List<Color>,
    selectedColor: Color,
    onColorSelect: (Color) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 36.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(colors) { color ->
            val selected = color == selectedColor

            Box(
                modifier = Modifier
                    .size(44.dp)                 // ✅ square grid cell / touch target
                    .clickable { onColorSelect(color) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)             // ✅ visual circle (fixed)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color =
                                if (selected)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}
