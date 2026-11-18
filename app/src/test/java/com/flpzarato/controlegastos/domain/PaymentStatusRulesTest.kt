package com.flpzarato.controlegastos.domain

import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentStatusRulesTest {

    @Test
    fun `credito avulso nunca e baixado automaticamente`() {
        val result = PaymentStatusRules.autoMarksAsPaidOnCreation(
            paymentMethod = PaymentMethod.CREDITO,
            isFromRecurringTemplate = false,
        )
        assertEquals(false, result)
    }

    @Test
    fun `credito recorrente nunca e baixado automaticamente`() {
        val result = PaymentStatusRules.autoMarksAsPaidOnCreation(
            paymentMethod = PaymentMethod.CREDITO,
            isFromRecurringTemplate = true,
        )
        assertEquals(false, result)
    }

    @Test
    fun `nao credito avulso e baixado automaticamente`() {
        val result = PaymentStatusRules.autoMarksAsPaidOnCreation(
            paymentMethod = PaymentMethod.PIX,
            isFromRecurringTemplate = false,
        )
        assertEquals(true, result)
    }

    @Test
    fun `nao credito recorrente nunca e baixado automaticamente`() {
        val result = PaymentStatusRules.autoMarksAsPaidOnCreation(
            paymentMethod = PaymentMethod.DEBITO,
            isFromRecurringTemplate = true,
        )
        assertEquals(false, result)
    }

    @Test
    fun `credito nao pode ser marcado como pago manualmente`() {
        val result = PaymentStatusRules.canManuallyMarkPaid(PaymentMethod.CREDITO)
        assertEquals(false, result)
    }

    @Test
    fun `dinheiro debito e pix podem ser marcados como pagos manualmente`() {
        assertEquals(true, PaymentStatusRules.canManuallyMarkPaid(PaymentMethod.DINHEIRO))
        assertEquals(true, PaymentStatusRules.canManuallyMarkPaid(PaymentMethod.DEBITO))
        assertEquals(true, PaymentStatusRules.canManuallyMarkPaid(PaymentMethod.PIX))
    }
}
