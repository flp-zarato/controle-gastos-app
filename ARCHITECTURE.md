# Decisões de arquitetura

Referência mecânica (estrutura de pastas, diagramas, fluxo de dados) já é gerada
automaticamente e mantida em dia pelo DeepWiki:
**https://deepwiki.com/flp-zarato/controle-gastos-app**

Este arquivo cobre só o que não é óbvio lendo o código nem aparece no README
(que foca em setup) — o *porquê* de algumas escolhas, para não se perder quando
o projeto for revisitado depois de um tempo parado.

## Google Sheets é a fonte da verdade; Room é só um cache

O Room (`AppDatabase`) existe unicamente para o app funcionar offline — ele nunca
é a fonte de dados "real". Por isso:

- `DatabaseModule` usa `fallbackToDestructiveMigration()` em vez de `Migration`
  escritas à mão: um bump de versão do schema simplesmente apaga e resincroniza
  do zero a partir da planilha, sem risco de perder dado (ele sempre volta do
  Sheets no próximo sync).
- Qualquer coluna nova numa aba (`Observacao`, `RecorrenteId`,
  `MesesAntecedencia`, ...) é sempre **anexada ao final** do range em
  `SheetSchema`, nunca inserida no meio — assim uma planilha já em uso por
  quem já tem o app continua funcionando sem precisar reordenar nada.

## Resolução de conflito: "local wins until synced"

Dois celulares podem editar ao mesmo tempo. A regra adotada (em
`SheetsSyncRepository`) é: um registro com edição local pendente (`dirty = true`)
nunca é sobrescrito por um pull remoto até que o próprio push dele tenha
sucesso. Ou seja, a última edição *sincronizada* vence, mas uma edição local
ainda não enviada nunca é descartada silenciosamente. O trade-off aceito: se os
dois editarem o mesmo registro *antes* de qualquer um sincronizar, quem
sincronizar primeiro "ganha" — não há merge de campo a campo. Para o uso real do
app (casal, poucos lançamentos simultâneos) esse risco foi considerado aceitável
na hora de simplificar a implementação.

## Sync: por que duas chamadas no máximo por aba

Pensado para economizar quota da Sheets API: todo o lote de linhas dirty de uma
aba que já existem na planilha vai num único `values:batchUpdate`; todas as
linhas novas (ainda não existentes) vão num único `values:append`. Nunca uma
chamada HTTP por linha. Cada registro só é marcado `synced` depois que a
chamada que o contém teve sucesso — se a chamada falhar, ele continua `dirty` e
entra de novo no próximo sync.

## Por que o status de sync não vem do WorkManager

`SyncStatusRepository` é um `StateFlow` em memória, escrito direto pelo
`SheetsSyncWorker`, e não derivado de `WorkInfo`. Motivo: todo erro de sync
retorna `Result.retry()` (de propósito, para o WorkManager tentar de novo
sozinho), então o `WorkInfo` nunca chega a `FAILED` e não carrega informação
sobre *por que* falhou. A UI precisa diferenciar "falha de autenticação, peça
pra logar de novo" de "falha transitória, vai tentar de novo" — por isso esse
estado é mantido à parte.

## Despesas recorrentes: geradas dentro do próprio sync, não por um Worker separado

`launchDueRecurringExpenses()` roda dentro do mesmo `sync()` que faz pull/push,
em vez de um Worker dedicado. Isso evita agendar um segundo sync inteiro só pra
pegar as linhas recém-geradas: elas já nascem `dirty` e são pegas pelo passo de
push que vem a seguir, na mesma passada.

## Pago/a pagar em Crédito: travado na camada de domínio, não só na UI

A regra é: um lançamento em Crédito nunca vira "pago" por toggle individual —
só em lote, pelo botão "Marcar fatura como paga" (`ExpenseRepository.markInvoicePaid`).
A primeira implementação dessa regra ficou só na UI (o `onClick` do chip em
cada tela decidia se chamava `togglePaid`), e um lugar foi esquecido
(`InvoiceDetailScreen` permitia marcar item a item, por fora do botão em
lote). Por isso a trava real está em `PaymentStatusRules.canManuallyMarkPaid`
e é aplicada em `ExpenseRepository.markPaid`, que lança exceção para um id de
Crédito — a UI também bloqueia o clique (defesa em profundidade), mas a
garantia que importa é a da camada de dados, não a de cada tela lembrar de
checar a forma de pagamento.

## Agendamento: imediato + periódico, nunca só um dos dois

`requestImmediateSync()` dispara assim que há uma escrita local (resposta
rápida para quem está usando o app). `schedulePeriodicSync()` (30 min) existe
para o caso de um celular ficar parado sem gerar nenhuma escrita local — sem
ele, as alterações do outro celular só apareceriam na próxima vez que esse
usuário abrisse o app e salvasse algo.
