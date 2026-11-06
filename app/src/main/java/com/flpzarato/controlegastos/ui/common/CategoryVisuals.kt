package com.flpzarato.controlegastos.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

private val categoryPalette = listOf(
    Color(0xFF2E7D32), Color(0xFF1565C0), Color(0xFFEF6C00), Color(0xFF6A1B9A),
    Color(0xFFC62828), Color(0xFF00838F), Color(0xFFAD1457), Color(0xFF4E342E),
)

/**
 * Stable color per category, derived from its id (never its name, which can be edited)
 * so the same category always renders the same color across the list, the dashboard
 * chart and its legend, without needing a color field/migration on [CategoryEntity].
 */
fun categoryColor(categoryId: String): Color {
    val index = (categoryId.hashCode() and 0x7fffffff) % categoryPalette.size
    return categoryPalette[index]
}

private val defaultCategoryIcons = mapOf(
    "Alimentação" to Icons.Filled.Fastfood,
    "Transporte" to Icons.Filled.DirectionsCar,
    "Moradia" to Icons.Filled.Home,
    "Saúde" to Icons.Filled.LocalHospital,
    "Lazer" to Icons.Filled.SportsEsports,
)

/** The seeded default categories get a fitting icon; any custom category falls back to a neutral tag. */
fun categoryIcon(categoryName: String): ImageVector = defaultCategoryIcons[categoryName] ?: Icons.Filled.Label
