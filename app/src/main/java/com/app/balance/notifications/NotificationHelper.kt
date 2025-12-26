package com.app.balance.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.app.balance.InicioActivity
import com.app.balance.R

class NotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID_SAVINGS = "balance_savings_channel"
        const val CHANNEL_ID_PERSISTENT = "balance_persistent_channel"
        const val NOTIFICATION_ID_SAVINGS = 1001
        
        const val THRESHOLD_EXCELLENT = 80
        const val THRESHOLD_GOOD = 50
        const val THRESHOLD_WARNING = 30
        const val THRESHOLD_DANGER = 20
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal para alertas normales
            val savingsChannel = NotificationChannel(
                CHANNEL_ID_SAVINGS,
                "Alertas de Ahorro",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones sobre el estado de tu ahorro"
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Canal para notificaciones persistentes (importancia baja para no molestar)
            val persistentChannel = NotificationChannel(
                CHANNEL_ID_PERSISTENT,
                "Estado de Ahorro (Fijo)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación fija con el estado de tu ahorro"
                enableVibration(false)
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(savingsChannel)
            notificationManager.createNotificationChannel(persistentChannel)
        }
    }
    
    fun mostrarNotificacionAhorro(
        porcentajeAhorro: Float,
        montoAhorro: Double,
        codigoDivisa: String,
        esPersistente: Boolean = false
    ) {
        if (!tienePermisoNotificaciones()) return
        
        val (titulo, mensaje) = obtenerContenidoNotificacion(porcentajeAhorro, montoAhorro, codigoDivisa)
        
        val intent = Intent(context, InicioActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Usar canal diferente según si es persistente o no
        val channelId = if (esPersistente) CHANNEL_ID_PERSISTENT else CHANNEL_ID_SAVINGS
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_billetera)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setContentIntent(pendingIntent)
            .setColor(obtenerColorNotificacion(porcentajeAhorro))
            .setOnlyAlertOnce(true) // No repetir sonido si se actualiza
        
        if (esPersistente) {
            builder.setOngoing(true)  // No se puede deslizar para eliminar
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(false)
        } else {
            builder.setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
        }
        
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SAVINGS, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    private fun tienePermisoNotificaciones(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    private fun obtenerContenidoNotificacion(
        porcentajeAhorro: Float,
        montoAhorro: Double,
        codigoDivisa: String
    ): Pair<String, String> {
        val montoFormateado = "$codigoDivisa ${String.format("%.2f", montoAhorro)}"
        val porcentajeFormateado = String.format("%.1f", porcentajeAhorro)
        
        return when {
            porcentajeAhorro >= THRESHOLD_EXCELLENT -> Pair(
                "¡Excelente ahorro! 🎉",
                "Tienes $porcentajeFormateado% ahorrado ($montoFormateado). ¡Sigue así!"
            )
            porcentajeAhorro >= THRESHOLD_GOOD -> Pair(
                "Buen progreso 👍",
                "Ahorro: $porcentajeFormateado% ($montoFormateado). Vas bien."
            )
            porcentajeAhorro >= THRESHOLD_WARNING -> Pair(
                "Atención ⚠️",
                "Ahorro: $porcentajeFormateado% ($montoFormateado). Considera reducir gastos."
            )
            porcentajeAhorro > 0 -> Pair(
                "¡Alerta! 🔴",
                "Solo $porcentajeFormateado% de ahorro ($montoFormateado). Revisa tus gastos."
            )
            else -> Pair(
                "Sin ahorro ⚠️",
                "Tu ahorro está en $montoFormateado. Ajusta tu presupuesto."
            )
        }
    }
    
    private fun obtenerColorNotificacion(porcentajeAhorro: Float): Int {
        return when {
            porcentajeAhorro >= THRESHOLD_EXCELLENT -> ContextCompat.getColor(context, R.color.success_green)
            porcentajeAhorro >= THRESHOLD_GOOD -> ContextCompat.getColor(context, R.color.gold)
            porcentajeAhorro >= THRESHOLD_WARNING -> ContextCompat.getColor(context, R.color.warning_orange)
            else -> ContextCompat.getColor(context, R.color.error_red)
        }
    }
    
    fun cancelarNotificacionAhorro() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SAVINGS)
    }
    
    fun cancelarTodasNotificaciones() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
