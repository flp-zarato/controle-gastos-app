package com.flpzarato.controlegastos.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flpzarato.controlegastos.ui.cards.CardsScreen
import com.flpzarato.controlegastos.ui.categories.CategoriesScreen
import com.flpzarato.controlegastos.ui.expense.AddExpenseScreen
import com.flpzarato.controlegastos.ui.home.HomeScreen
import com.flpzarato.controlegastos.ui.invoice.InvoiceDetailScreen
import com.flpzarato.controlegastos.ui.invoice.InvoiceListScreen
import com.flpzarato.controlegastos.ui.listing.ExpenseListScreen
import com.flpzarato.controlegastos.ui.login.LoginScreen
import com.flpzarato.controlegastos.ui.recurring.RecurringFormScreen
import com.flpzarato.controlegastos.ui.recurring.RecurringListScreen

object Routes {
    const val LOGIN = "login"
    const val INICIO = "inicio"
    const val LANCAMENTOS = "lancamentos"
    const val NOVO_GASTO = "novo_gasto"
    const val EDITAR_GASTO = "editar_gasto"
    const val CATEGORIAS = "categorias"
    const val CARTOES = "cartoes"
    const val RECORRENTES = "recorrentes"
    const val NOVO_RECORRENTE = "novo_recorrente"
    const val EDITAR_RECORRENTE = "editar_recorrente"
    const val FATURAS = "faturas"
    const val FATURA_DETALHE = "fatura_detalhe"

    fun editarGastoRoute(groupId: String) = "$EDITAR_GASTO/$groupId"
    fun editarRecorrenteRoute(id: String) = "$EDITAR_RECORRENTE/$id"
    fun faturasRoute(cardId: String) = "$FATURAS/$cardId"
    fun faturaDetalheRoute(cardId: String, competence: String) = "$FATURA_DETALHE/$cardId/$competence"
}

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(Routes.INICIO, "Início", Icons.Filled.Home),
    BottomDestination(Routes.LANCAMENTOS, "Lançamentos", Icons.Filled.List),
    BottomDestination(Routes.RECORRENTES, "Recorrentes", Icons.Filled.Autorenew),
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
                    navController.navigate(Routes.INICIO) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                })
            }
            composable(Routes.INICIO) { HomeScreen() }
            composable(Routes.LANCAMENTOS) {
                ExpenseListScreen(
                    onAddExpense = { navController.navigate(Routes.NOVO_GASTO) },
                    onEditExpense = { groupId -> navController.navigate(Routes.editarGastoRoute(groupId)) },
                )
            }
            composable(Routes.NOVO_GASTO) {
                AddExpenseScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "${Routes.EDITAR_GASTO}/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            ) {
                AddExpenseScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.CATEGORIAS) { CategoriesScreen() }
            composable(Routes.CARTOES) {
                CardsScreen(onOpenInvoices = { cardId -> navController.navigate(Routes.faturasRoute(cardId)) })
            }
            composable(Routes.RECORRENTES) {
                RecurringListScreen(
                    onAddRecurring = { navController.navigate(Routes.NOVO_RECORRENTE) },
                    onEditRecurring = { id -> navController.navigate(Routes.editarRecorrenteRoute(id)) },
                )
            }
            composable(Routes.NOVO_RECORRENTE) {
                RecurringFormScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "${Routes.EDITAR_RECORRENTE}/{recurringId}",
                arguments = listOf(navArgument("recurringId") { type = NavType.StringType }),
            ) {
                RecurringFormScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "${Routes.FATURAS}/{cardId}",
                arguments = listOf(navArgument("cardId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getString("cardId").orEmpty()
                InvoiceListScreen(
                    onOpenInvoice = { competence ->
                        navController.navigate(Routes.faturaDetalheRoute(cardId, competence))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "${Routes.FATURA_DETALHE}/{cardId}/{competence}",
                arguments = listOf(
                    navArgument("cardId") { type = NavType.StringType },
                    navArgument("competence") { type = NavType.StringType },
                ),
            ) {
                InvoiceDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
