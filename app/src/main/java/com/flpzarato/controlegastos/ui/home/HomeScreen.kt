package com.flpzarato.controlegastos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flpzarato.controlegastos.ui.common.CenteredLoadingIndicator
import com.flpzarato.controlegastos.ui.common.ControleGastosTopBar
import com.flpzarato.controlegastos.ui.common.DonutChart
import com.flpzarato.controlegastos.ui.common.DonutSlice
import com.flpzarato.controlegastos.ui.common.EmptyState
import com.flpzarato.controlegastos.ui.common.categoryColor
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val currencyFormat = remember(Locale("pt", "BR")) { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    Scaffold(
        topBar = { ControleGastosTopBar(title = "Início") },
    ) { padding ->
        if (state.isLoading) {
            CenteredLoadingIndicator(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        if (state.byCategory.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.List,
                message = "Nenhum gasto neste mês ainda",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column {
                    Text(
                        state.monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        currencyFormat.format(state.total),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(
                        slices = state.byCategory.map { DonutSlice(it.total, categoryColor(it.categoryId)) },
                        modifier = Modifier.size(120.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        state.byCategory.forEach { category ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 3.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(categoryColor(category.categoryId)),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${category.categoryName} · ${currencyFormat.format(category.total)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text("Por pessoa", style = MaterialTheme.typography.titleMedium)
            }

            items(state.byUser, key = { it.email }) { user ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(user.displayName)
                        Text(currencyFormat.format(user.total), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
