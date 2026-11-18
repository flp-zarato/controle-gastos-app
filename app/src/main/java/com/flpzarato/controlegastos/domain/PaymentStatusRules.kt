package com.flpzarato.controlegastos.domain

import com.flpzarato.controlegastos.data.local.entity.PaymentMethod

object PaymentStatusRules {
    /**
     * Credito nunca e baixado automaticamente (depende da fatura ser paga em lote). Um
     * lancamento gerado por um Recorrente tambem nunca e baixado automaticamente, mesmo em
     * Debito/Dinheiro/Pix - ele representa uma previsao que pode ser paga bem depois de ser
     * lancada.
     */
    fun autoMarksAsPaidOnCreation(paymentMethod: PaymentMethod, isFromRecurringTemplate: Boolean): Boolean =
        paymentMethod != PaymentMethod.CREDITO && !isFromRecurringTemplate

    /** Crédito só pode virar pago em lote, via fatura — nunca por toggle manual individual. */
    fun canManuallyMarkPaid(paymentMethod: PaymentMethod): Boolean =
        paymentMethod != PaymentMethod.CREDITO
}
