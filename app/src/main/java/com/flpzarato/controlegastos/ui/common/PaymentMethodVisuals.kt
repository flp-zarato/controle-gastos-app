package com.flpzarato.controlegastos.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.ui.graphics.vector.ImageVector
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod

fun paymentMethodIcon(method: PaymentMethod): ImageVector = when (method) {
    PaymentMethod.DINHEIRO -> Icons.Filled.Payments
    PaymentMethod.DEBITO -> Icons.Filled.AccountBalanceWallet
    PaymentMethod.CREDITO -> Icons.Filled.CreditCard
    PaymentMethod.PIX -> Icons.Filled.Bolt
}

fun paymentMethodLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.DINHEIRO -> "Dinheiro"
    PaymentMethod.DEBITO -> "Débito"
    PaymentMethod.CREDITO -> "Crédito"
    PaymentMethod.PIX -> "Pix"
}
