package com.flpzarato.controlegastos.data.remote

/**
 * The shared Google Sheet has four tabs, each a "tidy" table with a header
 * row and one data row per record. The app only ever reads/writes columns
 * A onward on each tab; extra analysis tabs/pivot tables the person adds
 * are left untouched since every range used here is scoped to a named tab.
 */
object SheetSchema {
    const val TAB_LANCAMENTOS = "Lançamentos"
    const val TAB_CATEGORIAS = "Categorias"
    const val TAB_CARTOES = "Cartões"
    const val TAB_USUARIOS = "Usuários"

    // Column order matches ExpenseRowMapper.
    const val RANGE_LANCAMENTOS = "$TAB_LANCAMENTOS!A2:L"
    val HEADER_LANCAMENTOS = listOf(
        "ID", "DataCompra", "Competencia", "Descricao", "CategoriaId",
        "ValorParcela", "ValorTotalOriginal", "Parcela", "FormaPagamento",
        "CartaoId", "QuemLancou", "Deletado",
    )

    const val RANGE_CATEGORIAS = "$TAB_CATEGORIAS!A2:C"
    val HEADER_CATEGORIAS = listOf("ID", "Nome", "Deletado")

    const val RANGE_CARTOES = "$TAB_CARTOES!A2:G"
    val HEADER_CARTOES = listOf(
        "ID", "Apelido", "TitularEmail", "Banco", "DiaFechamento", "DiaVencimento", "Deletado",
    )

    const val RANGE_USUARIOS = "$TAB_USUARIOS!A2:B"
    val HEADER_USUARIOS = listOf("Email", "Nome")

    fun rowRange(tab: String, row: Int, lastColumn: String): String = "$tab!A$row:$lastColumn$row"
}
