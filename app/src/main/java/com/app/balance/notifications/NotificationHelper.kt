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

/**
 * Helper para manejar notificaciones de Balance+
 * Incluye alertas basadas en porcentaje de ahorro
 */
class NotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID_SAVINGS = "balance_savings_channel"
        const val CHANNEL_ID_REMINDERS = "balance_reminders_channel"
        
        const val NOTIFICATION_ID_SAVINGS = 1001
        const val NOTIFICATION_ID_REMINDER = 1002
        
        // Umbrales de alerta
        const val THRESHOLD_EXCELLENT = 80  // > 80% ahorro
        const val THRESHOLD_GOOD = 50       // 50-80% ahorro
        const val THRESHOLD_WARNING = 30    // 30-50% ahorro
        const val THRESHOLD_DANGER = 20     // < 20% ahorro
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Crea los canales de notificación (requerido para Android 8+)
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Canal para alertas de ahorro
            val savingsChannel = NotificationChannel(
                CHANNEL_ID_SAVINGS,
                "Alertas de Ahorro",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones sobre el estado de tu ahorro"
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Canal para recordatorios
            val remindersChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Recordatorios",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Recordatorios para registrar tus gastos"
                enableVibration(false)
            }
            
            notificationManager.createNotificationChannel(savingsChannel)
            notificationManager.createNotificationChannel(remindersChannel)
        }
    }
    
    /**
     * Muestra notificación basada en el porcentaje de ahorro
     * @param porcentajeAhorro Porcentaje actual de ahorro (0-100)
     * @param montoAhorro Monto actual de ahorro
     * @param codigoDivisa Código de la divisa (PEN, USD, etc.)
     * @param esPersistente Si la notificación debe ser fija en la barra
     */
    fun mostrarNotificacionAhorro(
        porcentajeAhorro: Float,
        montoAhorro: Double,
        codigoDivisa: String,
        esPersistente: Boolean = false
    ) {
        // Verificar permisos en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        
        val (titulo, mensaje, icono) = obtenerContenidoNotificacion(
            porcentajeAhorro, 
            montoAhorro, 
            codigoDivisa
        )
        
        // Intent para abrir la app al tocar la notificación
        val intent = Intent(context, InicioActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SAVINGS)
            .setSmallIcon(R.drawable.ic_billetera)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(!esPersistente)
            .setOngoing(esPersistente)
            .setColor(ContextCompat.getColor(context, R.color.gold))
        
        // Color según el estado
        when {
            porcentajeAhorro >= THRESHOLD_EXCELLENT -> {
                builder.setColor(ContextCompat.getColor(context, R.color.success_green))
            }
            porcentajeAhorro >= THRESHOLD_GOOD -> {
                builder.setColor(ContextCompat.getColor(context, R.color.gold))
            }
            porcentajeAhorro >= THRESHOLD_WARNING -> {
                builder.setColor(ContextCompat.getColor(context, R.color.warning_orange))
            }
            else -> {
                builder.setColor(ContextCompat.getColor(context, R.color.error_red))
            }
        }
        
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_SAVINGS, builder.build())
        }
    }
    
    /**
     * Obtiene el contenido de la notificación según el porcentaje de ahorro
     */
    private fun obtenerContenidoNotificacion(
        porcentajeAhorro: Float,
        montoAhorro: Double,
        codigoDivisa: String
    ): Triple<String, String, Int> {
        val montoFormateado = "$codigoDivisa ${String.format("%.2f", montoAhorro)}"
        
        return when {
            porcentajeAhorro >= THRESHOLD_EXCELLENT -> Triple(
                "¡Excelente ahorro! 🎉",
                "Has ahorrado el ${String.format("%.1f", porcentajeAhorro)}% de tu balance ($montoFormateado). ¡Sigue así!",
                R.drawable.ic_check_circle
            )
            porcentajeAhorro >= THRESHOLD_GOOD -> Triple(
                "Buen progreso 👍",
                "Tu ahorro es del ${String.format("%.1f", porcentajeAhorro)}% ($montoFormateado). Vas por buen camino.",
                R.drawable.ic_billetera
            )
            porcentajeAhorro >= THRESHOLD_WARNING -> Triple(
                "Atención con tus gastos ⚠️",
                "Tu ahorro ha bajado al ${String.format("%.1f", porcentajeAhorro)}% ($montoFormateado). Considera reducir gastos.",
                R.drawable.ic_billetera
            )
            porcentajeAhorro >= THRESHOLD_DANGER -> Triple(
                "¡Alerta de ahorro! 🔴",
                "Solo tienes ${String.format("%.1f", porcentajeAhorro)}% de ahorro ($montoFormateado). Revisa tus gastos.",
                R.drawable.ic_billetera
            )
            else -> Triple(
                "⚠️ Sin ahorro disponible",
                "Tu ahorro está en $montoFormateado. Es momento de ajustar tu presupuesto.",
                R.drawable.ic_billetera
            )
        }
    }
    
    /**
     * Muestra recordatorio para registrar gastos
     */
    fun mostrarRecordatorioGastos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        
        val intent = Intent(context, InicioActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_billetera)
            .setContentTitle("📝 Registra tus gastos")
            .setContentText("No olvides registrar los gastos del día para mantener tu balance actualizado.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, R.color.gold))
        
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_REMINDER, builder.build())
        }
    }
    
    /**
     * Cancela la notificación de ahorro
     */
    fun cancelarNotificacionAhorro() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SAVINGS)
    }
    
    /**
     * Cancela todas las notificaciones
     */
    fun cancelarTodasNotificaciones() {
        NotificationManagerCompat.from(context).cancelAll()
    }
    
    /**
     * Verifica si las notificaciones están habilitadas
     */
    fun notificacionesHabilitadas(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
