package com.app.balance.ui

interface BalanceUpdateListener {
    fun onBalanceUpdated(nuevoBalance: Double, codigoDivisa: String)
}