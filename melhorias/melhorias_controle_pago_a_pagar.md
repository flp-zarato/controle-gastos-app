# Prompt para o Claude Code — Controle de "Pago" / "A pagar"

## Contexto

App Android (Kotlin + Jetpack Compose) em `controle-gastos-app`, offline-first,
com Room como cache local e Google Sheets como fonte da verdade (sync via
`SheetsSyncRepository`). Vamos adicionar controle de pagamento por lançamento:
saber o que já foi pago e o que ainda está em aberto, por pessoa.

Regras de negócio definidas (não mudar sem perguntar):

1. **Granularidade é por parcela/linha (`ExpenseEntity`), nunca por grupo
   parcelado inteiro.** Uma compra parcelada em 3x pode ter a 1ª parcela paga
   e a 2ª/3ª ainda em aberto.
2. **Cartão de crédito (`PaymentMethod.CREDITO`) nunca tem toggle manual de
   "pago" no lançamento individual.** A parcela só vira `paid = true` quando a
   fatura inteira daquele cartão/competência for marcada como paga (ação em
   lote na tela de fatura). Isso reintroduz, com propósito mais específico, a
   funcionalidade de "marcar fatura como paga" que o README atual lista como
   fora de escopo — atualizar aquela seção (ver Critério 9).
3. **Dinheiro/Débito/Pix: a baixa é automática (`paid = true` já na criação)
   — mas só para lançamentos avulsos** (criados em "Novo gasto"), nunca para
   lançamentos gerados a partir de um Recorrente (`recurringTemplateId != null`).
   Um recorrente em Pix/Débito/Dinheiro sempre nasce `paid = false`, porque
   representa uma previsão/controle que pode ser paga depois do lançamento
   (ex.: financiamento da casa, lançado como controle antes do PIX real).
4. **Baixa automática exige aceite único e prévio do usuário** (ver critério
   4), guardado em preferência — depois do aceite, não pergunta mais.
5. **Desmarcar "pago" sempre exige confirmação via `AlertDialog`** (mesmo
   padrão de `ExpenseListScreen`/`RecurringListScreen`). Marcar como pago
   (manual ou em lote pela fatura) não precisa de confirmação.

## Objetivo

1. Adicionar campo `paid: Boolean = false` a `ExpenseEntity` e espelhar na
   planilha compartilhada (nova coluna "Pago", **anexada ao final**, nunca
   reordenando colunas existentes — mesmo padrão já usado para `Observacao`/
   `RecorrenteId`).
2. Implementar a regra de baixa automática (critérios 2 e 3) como uma função
   pura e testável em `domain/`, decidida separadamente do aceite de UI.
3. Fluxo de aceite único (uma vez na vida do app) antes de aplicar a baixa
   automática pela primeira vez, guardado em `UserPreferences`.
4. Botão "Marcar fatura como paga" em `InvoiceDetailScreen`, aplicando
   `paid = true` em lote a todas as parcelas não pagas daquela
   competência/cartão.
5. Indicador visual de status ("Pago" / "Em aberto") nos itens de
   `ExpenseListScreen` e `InvoiceDetailScreen`, com toggle para desmarcar
   (com confirmação) tanto em lançamentos de cartão quanto não-cartão.
6. Nova tela "Pagamentos por pessoa", acessível clicando no nome de uma
   pessoa na seção "Por pessoa" da tela Início, com duas abas ("Pagos" /
   "A pagar") filtrando os lançamentos daquela pessoa.
7. Atualizar o README: nova coluna na tabela de colunas da aba
   "Lançamentos", e revisar a seção "Fora de escopo" removendo a menção a
   "marcar fatura como paga" (substituir pela explicação da nova
   funcionalidade e seu propósito).

## Arquivos envolvidos

### Dados e sync
- `app/src/main/java/com/flpzarato/controlegastos/data/local/entity/ExpenseEntity.kt`
  — adicionar `val paid: Boolean = false`.
- `app/src/main/java/com/flpzarato/controlegastos/data/local/AppDatabase.kt`
  — bump `version = 6` → `version = 7` (segue o padrão existente de
  `fallbackToDestructiveMigration()`, não escrever `Migration` manual).
- `app/src/main/java/com/flpzarato/controlegastos/data/local/dao/ExpenseDao.kt`
  — adicionar:
  - `suspend fun markPaid(id: String)` → `UPDATE expenses SET paid = 1, dirty = 1 WHERE id = :id`
  - `suspend fun markUnpaid(id: String)` → `UPDATE expenses SET paid = 0, dirty = 1 WHERE id = :id`
  - `suspend fun markInvoicePaid(cardId: String, competenceMonth: String)` →
    `UPDATE expenses SET paid = 1, dirty = 1 WHERE cardId = :cardId AND competenceMonth = :competenceMonth AND deleted = 0 AND paid = 0`
- `app/src/main/java/com/flpzarato/controlegastos/data/remote/SheetSchema.kt`
  — `RANGE_LANCAMENTOS` de `"A2:N"` para `"A2:O"`; `HEADER_LANCAMENTOS` ganha
  `"Pago"` no final da lista.
- `app/src/main/java/com/flpzarato/controlegastos/data/remote/SheetMappers.kt`
  — `ExpenseEntity.toRow()` adiciona `paid.toString()` ao final;
  `List<String>.toExpenseEntity()` lê `cell(14)?.toBoolean() ?: false` para `paid`.
- `app/src/main/java/com/flpzarato/controlegastos/data/repository/SheetsSyncRepository.kt`
  — em `pushDirtyExpenses`, o `lastColumn` passado para `pushDirtyBatch` muda
  de `"N"` para `"O"`.
- `app/src/main/java/com/flpzarato/controlegastos/data/repository/ExpenseRepository.kt`
  — `addExpense`/`insertExpenseRows` ganham parâmetro `paid: Boolean = false`,
  aplicado ao `ExpenseEntity` criado (nas duas variantes, crédito e não-crédito).
  **Importante:** em `updateExpense`, que recria o grupo inteiro chamando
  `addExpense` internamente, **preservar o `paid` do grupo original** (buscar
  via `expenseDao.getByGroup(oldGroupId)` antes de marcar deletado) em vez de
  recalcular pela regra de baixa automática — editar um lançamento não deve
  resetar nem reaplicar status de pagamento.
- O ponto em `SheetsSyncRepository.launchDueRecurringExpenses()` que chama
  `expenseRepository.insertExpenseRows(...)` não passa `paid` (fica `false`,
  default) — não precisa mudança, só confirmar que continua assim.

### Regra de negócio (nova, em domain/)
- Criar `app/src/main/java/com/flpzarato/controlegastos/domain/PaymentStatusRules.kt`:
  ```kotlin
  object PaymentStatusRules {
      /**
       * Crédito nunca é baixado automaticamente (depende da fatura ser paga).
       * Um lançamento gerado por um Recorrente também nunca é baixado
       * automaticamente, mesmo em Débito/Dinheiro/Pix — ele representa uma
       * previsão que pode ser paga bem depois de ser lançada.
       */
      fun autoMarksAsPaidOnCreation(paymentMethod: PaymentMethod, isFromRecurringTemplate: Boolean): Boolean =
          paymentMethod != PaymentMethod.CREDITO && !isFromRecurringTemplate
  }
  ```
- Adicionar teste em `app/src/test/java/com/flpzarato/controlegastos/domain/PaymentStatusRulesTest.kt`
  cobrindo as 4 combinações relevantes (crédito avulso, crédito recorrente,
  não-crédito avulso, não-crédito recorrente), seguindo o estilo de
  `InstallmentCalculatorTest.kt` (JUnit 4, nomes de teste em português).

### Preferência de aceite único
- `app/src/main/java/com/flpzarato/controlegastos/data/prefs/UserPreferences.kt`
  — adicionar chave `AUTO_PAID_ACCEPTED` (`booleanPreferencesKey`), flow
  `val autoPaidAccepted: Flow<Boolean>` e `suspend fun saveAutoPaidAccepted()`.

### Tela "Novo gasto" (aceite único + aplicação da baixa automática)
- `app/src/main/java/com/flpzarato/controlegastos/ui/expense/AddExpenseViewModel.kt`
  — em `AddExpenseFormState`, novo campo `val autoPaidPrompt: Boolean = false`.
  Em `attemptSave`, **só quando `editingGroupId == null`** (criação, nunca
  edição) e `PaymentStatusRules.autoMarksAsPaidOnCreation(paymentMethod, isFromRecurringTemplate = false)`
  for `true`: checar `userPreferences.autoPaidAccepted.first()`; se `false`,
  parar o salvamento e setar `autoPaidPrompt = true` (mesmo padrão do
  `invoiceWarningMonth` já existente — não salva até o usuário decidir).
  Novas funções `confirmAutoPaidPrompt()` (chama `userPreferences.saveAutoPaidAccepted()`,
  depois refaz o save already with `paid = true`) e `dismissAutoPaidPrompt()`
  (só fecha o diálogo, **não salva** — mesmo comportamento do "Revisar" do
  diálogo de fatura). Se a preferência já for `true`, aplica `paid = true`
  direto, sem diálogo. Passar o `paid` resultante para
  `expenseRepository.addExpense(...)`.
- `app/src/main/java/com/flpzarato/controlegastos/ui/expense/AddExpenseScreen.kt`
  — novo `AlertDialog` (mesmo estilo visual do `invoiceWarningMonth` já
  existente logo abaixo dele), texto explicando que lançamentos em
  Dinheiro/Débito/Pix serão marcados como pagos automaticamente (já que o
  valor sai da conta no ato), com botão de confirmar (chama
  `confirmAutoPaidPrompt`) e de cancelar/revisar (chama `dismissAutoPaidPrompt`).

### Tela "Novo/editar recorrente" (aviso informativo, não bloqueante)
- `app/src/main/java/com/flpzarato/controlegastos/ui/recurring/RecurringFormScreen.kt`
  — quando `form.paymentMethod != PaymentMethod.CREDITO`, adicionar um
  `Text` de apoio (estilo `bodySmall`, cor `onSurfaceVariant`, mesmo padrão
  de outros textos auxiliares da tela) logo abaixo do seletor de forma de
  pagamento, avisando algo como: "Lançamentos gerados por este recorrente
  sempre nascem como 'a pagar', mesmo em dinheiro/débito/pix — você confirma
  manualmente quando o pagamento acontecer de fato."

### Tela de fatura (marcar em lote + indicador por parcela)
- `app/src/main/java/com/flpzarato/controlegastos/ui/invoice/InvoiceDetailViewModel.kt`
  — adicionar à `InvoiceDetailUiState` algo como `val allPaid: Boolean` e
  `val paidCount: Int` (derivados de `filtered`), e um método
  `fun markInvoicePaid()` chamando um novo método equivalente em
  `ExpenseRepository` (`markInvoicePaid(cardId, competenceMonth)` → chama o
  DAO novo + `syncScheduler.requestImmediateSync()`), e `fun togglePaid(expenseId: String, paid: Boolean)`
  chamando `ExpenseRepository.markPaid`/`markUnpaid` (novos métodos no
  repository, mesmo padrão de `deleteGroup`: chamam o DAO e agendam sync).
- `app/src/main/java/com/flpzarato/controlegastos/ui/invoice/InvoiceDetailScreen.kt`
  — no topo (ao lado do "Total da fatura"), botão "Marcar fatura como paga"
  (some/desabilita se `state.allPaid`, mostra "Pago" e paidCount/total como
  texto pequeno em vez de botão). Em cada item da lista, adicionar um chip
  pequeno "Pago" (cor primária) ou "Em aberto" (cor `error`/`tertiary`,
  escolher o que já existir na paleta do tema) — tocar no chip de "Pago" abre
  o `AlertDialog` de confirmação de desmarcação (reaproveitar texto/estilo do
  `AlertDialog` de exclusão de `ExpenseListScreen`, adaptando a mensagem para
  "Remover a marcação de pago desse lançamento?"); tocar em "Em aberto" marca
  como pago direto, sem diálogo.

### Tela Lançamentos (mesmo indicador)
- `app/src/main/java/com/flpzarato/controlegastos/ui/listing/ExpenseListScreen.kt`
  e `ExpenseListViewModel.kt` — mesmo chip "Pago"/"Em aberto" e mesmo toggle
  (com confirmação só para desmarcar) descritos acima, no mesmo lugar visual
  onde já ficam os ícones de editar/excluir.

### Nova tela "Pagamentos por pessoa"
- Criar pacote `ui/payments/` com `PersonPaymentsScreen.kt` e
  `PersonPaymentsViewModel.kt`.
- `PersonPaymentsViewModel`: recebe `email` via `SavedStateHandle` (mesmo
  padrão de `InvoiceDetailViewModel`), usa
  `ExpenseRepository.observeCurrentAndPreviousMonth()` (já existe — traz
  todas as parcelas, não só a primeira de cada grupo) filtrando por
  `loggedByEmail == email`, dividido em duas listas (`paid` / `unpaid`), cada
  uma com total somado.
- `PersonPaymentsScreen`: mesmo padrão visual de `ExpenseListScreen` —
  `TabRow` com duas abas ("Pagos" / "A pagar"), um `LazyColumn` de
  `ElevatedCard` por aba, chip de status redundante pode ser omitido aqui já
  que a aba já informa o status; manter o toggle de desmarcar (com
  confirmação) na aba "Pagos".
- `ui/navigation/ControleGastosNavHost.kt` — nova rota
  `Routes.PAGAMENTOS_PESSOA` com helper
  `fun pagamentosPessoaRoute(email: String) = "$PAGAMENTOS_PESSOA/$email"`,
  `composable` com `navArgument("email")`.
- `ui/home/HomeScreen.kt` — adicionar parâmetro
  `onOpenPersonPayments: (String) -> Unit`, e tornar o `ElevatedCard` de cada
  pessoa em "Por pessoa" clicável (`onClick = { onOpenPersonPayments(user.email) }`),
  navegando para `Routes.pagamentosPessoaRoute(user.email)`.

### README
- Atualizar a tabela de colunas da aba `Lançamentos` (seção "Criar a
  planilha compartilhada"), adicionando `Pago` ao final da lista de colunas.
- Avisar explicitamente nessa seção que planilhas **já existentes** (como a
  do casal) precisam ter a coluna `Pago` adicionada manualmente na próxima
  vez que abrirem a planilha — o app não cria a coluna sozinho.
- Na seção "Fora de escopo", remover a frase sobre "marcar fatura como paga
  (isso continua só na planilha)" e substituir por uma frase curta explicando
  que isso passou a fazer parte do app, especificamente para alimentar o
  controle de pago/a pagar por pessoa (não é mais fora de escopo).

## Critérios de aceite

1. `ExpenseEntity` tem `paid: Boolean = false`; Room sobe pra versão 7 sem
   `Migration` manual (segue `fallbackToDestructiveMigration`).
2. `PaymentStatusRulesTest` cobre as 4 combinações e passa (`./gradlew testDebugUnitTest`).
3. Criar um gasto novo em Dinheiro/Débito/Pix, pela primeira vez no app,
   mostra o diálogo de aceite; confirmar salva como `paid = true` e grava a
   preferência; criar um segundo gasto nas mesmas condições não mostra mais
   o diálogo.
4. Criar um gasto novo no Crédito nunca mostra o diálogo de aceite nem nasce
   pago.
5. Um lançamento gerado por um Recorrente em Pix/Débito/Dinheiro nasce
   `paid = false`, mesmo com a preferência de aceite já concedida.
6. Editar um lançamento existente preserva o `paid` que ele já tinha antes da
   edição (crédito ou não).
7. Em `InvoiceDetailScreen`, "Marcar fatura como paga" aplica `paid = true` a
   todas as parcelas não pagas daquela competência/cartão em uma única ação;
   desmarcar qualquer parcela individual (lá ou em `ExpenseListScreen`/
   `PersonPaymentsScreen`) sempre pede confirmação antes.
8. Clicar no nome de uma pessoa na tela Início abre a tela nova com as abas
   "Pagos"/"A pagar" filtradas corretamente por quem lançou.
9. README atualizado conforme a seção acima.
10. `./gradlew assembleDebug` compila sem erros.
