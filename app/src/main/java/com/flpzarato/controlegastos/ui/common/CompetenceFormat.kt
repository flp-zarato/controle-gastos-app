package com.flpzarato.controlegastos.ui.common

import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Formats a "yyyy-MM" competência string as "Mês/Ano" in Portuguese, e.g. "Dezembro/2026". */
fun formatCompetenceLabel(yyyyMM: String): String {
    val yearMonth = YearMonth.parse(yyyyMM)
    val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
    return "${monthName.replaceFirstChar { it.uppercase() }}/${yearMonth.year}"
}
