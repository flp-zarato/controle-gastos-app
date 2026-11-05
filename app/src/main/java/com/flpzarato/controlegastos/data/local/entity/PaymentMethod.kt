package com.flpzarato.controlegastos.data.local.entity

enum class PaymentMethod {
    DINHEIRO, DEBITO, CREDITO, PIX;

    companion object {
        fun fromStorage(value: String): PaymentMethod =
            entries.firstOrNull { it.name == value } ?: DINHEIRO
    }
}
