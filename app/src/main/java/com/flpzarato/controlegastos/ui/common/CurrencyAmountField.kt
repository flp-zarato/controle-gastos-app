package com.flpzarato.controlegastos.ui.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private val brlFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

/** Parses a digits-only "amount in cents" string (e.g. "15050" -> R$150,50). Blank/invalid input returns null. */
fun centsStringToBigDecimal(digits: String): BigDecimal? =
    digits.toLongOrNull()?.let { BigDecimal(it).movePointLeft(2) }

/** Inverse of [centsStringToBigDecimal], used to pre-fill the field when editing an existing amount. */
fun bigDecimalToCentsString(value: BigDecimal): String =
    value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toBigInteger().toString()

/**
 * Digits-only money field that renders as Brazilian currency as you type — e.g. typing
 * "15050" shows "R$ 150,50". [digitsValue] holds only digits (cents); convert with
 * [centsStringToBigDecimal] when saving.
 */
@Composable
fun CurrencyAmountField(
    digitsValue: String,
    onDigitsChanged: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = digitsValue,
        onValueChange = { raw -> onDigitsChanged(raw.filter(Char::isDigit).take(12)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = CurrencyVisualTransformation,
        modifier = modifier,
    )
}

private object CurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val amount = centsStringToBigDecimal(digits) ?: BigDecimal.ZERO
        val formatted = brlFormat.format(amount)
        return TransformedText(
            AnnotatedString(formatted),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = formatted.length
                override fun transformedToOriginal(offset: Int) = digits.length
            },
        )
    }
}
