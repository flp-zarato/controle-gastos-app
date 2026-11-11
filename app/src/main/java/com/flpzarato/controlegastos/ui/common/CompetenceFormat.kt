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

// Fixed, locale-independent 3-letter lowercase abbreviations (no dot) — Java's own
// TextStyle.SHORT formatter for pt-BR includes a trailing "." and isn't guaranteed stable.
private val MONTH_ABBREVIATIONS_PT = listOf(
    "jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez",
)

/** e.g. firstCompetence "2026-12", count=3 -> "dez/jan/fev". */
fun installmentMonthsAbbreviation(firstCompetence: String, count: Int): String {
    val start = YearMonth.parse(firstCompetence)
    return (0 until count).joinToString("/") { offset ->
        MONTH_ABBREVIATIONS_PT[start.plusMonths(offset.toLong()).monthValue - 1]
    }
}
