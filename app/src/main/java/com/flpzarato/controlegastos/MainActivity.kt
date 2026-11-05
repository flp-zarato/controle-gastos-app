package com.flpzarato.controlegastos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flpzarato.controlegastos.ui.navigation.ControleGastosNavHost
import com.flpzarato.controlegastos.ui.theme.ControleGastosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ControleGastosTheme {
                ControleGastosNavHost()
            }
        }
    }
}
