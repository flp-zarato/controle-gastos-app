package com.flpzarato.controlegastos.ui.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavController

/**
 * Provided once at the root of [ControleGastosNavHost] so deeply-shared composables (like
 * [com.flpzarato.controlegastos.ui.common.ControleGastosTopBar], rendered from every screen)
 * can trigger navigation — e.g. back to Login on an auth failure — without threading a
 * NavController parameter through every single screen call site.
 */
val LocalNavController = compositionLocalOf<NavController?> { null }
