package com.flpzarato.controlegastos.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

/**
 * Shared app bar for every screen. Bottom-nav destinations (Lançamentos, Categorias,
 * Cartões) omit [onBack] since they are top-level and already reachable from the bottom
 * bar — a back arrow there would wrongly suggest a parent screen exists. Screens pushed
 * on top of them (e.g. Adicionar gasto) pass [onBack] so the user always has a visible
 * way out besides the system gesture/button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControleGastosTopBar(title: String, onBack: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
        },
    )
}
