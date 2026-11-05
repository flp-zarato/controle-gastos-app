package com.flpzarato.controlegastos.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flpzarato.controlegastos.ui.cards.CardsScreen
import com.flpzarato.controlegastos.ui.categories.CategoriesScreen
import com.flpzarato.controlegastos.ui.expense.AddExpenseScreen
import com.flpzarato.controlegastos.ui.listing.ExpenseListScreen
import com.flpzarato.controlegastos.ui.login.LoginScreen

object Routes {
    const val LOGIN = "login"
    const val LANCAMENTOS = "lancamentos"
    const val NOVO_GASTO = "novo_gasto"
    const val CATEGORIAS = "categorias"
    const val CARTOES = "cartoes"
}

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(Routes.LANCAMENTOS, "Lançamentos", Icons.Filled.List),
    BottomDestination(Routes.CATEGORIAS, "Categorias", Icons.Filled.Category),
    BottomDestination(Routes.CARTOES, "Cartões", Icons.Filled.CreditCard),
)

@Composable
fun ControleGastosNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomDestinations.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(onLoggedIn = {
                    navController.navigate(Routes.LANCAMENTOS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                })
            }
            composable(Routes.LANCAMENTOS) {
                ExpenseListScreen(onAddExpense = { navController.navigate(Routes.NOVO_GASTO) })
            }
            composable(Routes.NOVO_GASTO) {
                AddExpenseScreen(onSaved = { navController.popBackStack() })
            }
            composable(Routes.CATEGORIAS) { CategoriesScreen() }
            composable(Routes.CARTOES) { CardsScreen() }
        }
    }
}
