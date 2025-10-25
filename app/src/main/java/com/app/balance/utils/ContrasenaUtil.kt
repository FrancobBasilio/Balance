package com.app.balance.utils

import java.security.MessageDigest

object ContrasenaUtil {
    fun hashear(contrasena: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(contrasena.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verificar(contrasena: String, hash: String): Boolean {
        return hashear(contrasena) == hash
    }
}