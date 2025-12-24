package com.app.balance.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.app.balance.R
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.dao.TransaccionDAO
import com.app.balance.notifications.NotificationHelper
import com.app.balance.security.BiometricAuthManager
import com.app.balance.security.SecurePreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

class ConfiguracionFragment : Fragment() {

    private lateinit var swNotificaciones: MaterialSwitch
    private lateinit var swNotificacionFija: MaterialSwitch
    private lateinit var swTemaOscuro: MaterialSwitch
    private lateinit var swBiometria: MaterialSwitch
    private lateinit var tvBiometriaDesc: TextView
    private lateinit var tvNotificacionDesc: TextView
    
    private lateinit var biometricManager: BiometricAuthManager
    private lateinit var securePrefs: SecurePreferences
    private lateinit var notificationHelper: NotificationHelper
    
    private var isInitializing = true

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            habilitarNotificaciones()
        } else {
            swNotificaciones.isChecked = false
            Toast.makeText(
                requireContext(),
                "Permiso de notificaciones denegado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_configuracion, container, false)
        
        biometricManager = BiometricAuthManager(requireContext())
        securePrefs = SecurePreferences.getInstance(requireContext())
        notificationHelper = NotificationHelper(requireContext())

        initViews(view)
        cargarPreferencias()
        setupListeners()
        
        isInitializing = false

        return view
    }

    private fun initViews(view: View) {
        swNotificaciones = view.findViewById(R.id.swNotificaciones)
        swNotificacionFija = view.findViewById(R.id.swNotificacionFija)
        swTemaOscuro = view.findViewById(R.id.swTemaOscuro)
        swBiometria = view.findViewById(R.id.swBiometria)
        tvBiometriaDesc = view.findViewById(R.id.tvBiometriaDesc)
        tvNotificacionDesc = view.findViewById(R.id.tvNotificacionDesc)
    }

    private fun cargarPreferencias() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        val notificacionesActivas = prefs.getBoolean("NOTIFICACIONES_ACTIVAS", false)
        swNotificaciones.isChecked = notificacionesActivas
        
        val notificacionFija = prefs.getBoolean("NOTIFICACION_FIJA", false)
        swNotificacionFija.isChecked = notificacionFija
        swNotificacionFija.isEnabled = notificacionesActivas
        
        actualizarDescripcionNotificacion(notificacionesActivas)

        val modoOscuro = prefs.getBoolean("TEMA_OSCURO", false)
        swTemaOscuro.isChecked = modoOscuro
        
        configurarBiometria()
    }
    
    private fun actualizarDescripcionNotificacion(activas: Boolean) {
        if (activas) {
            tvNotificacionDesc.text = "Recibirás alertas según tu porcentaje de ahorro"
        } else {
            tvNotificacionDesc.text = "Activa para recibir alertas de tu ahorro"
        }
    }
    
    private fun configurarBiometria() {
        val biometricStatus = biometricManager.checkBiometricAvailability()
        
        when (biometricStatus) {
            BiometricAuthManager.BiometricStatus.AVAILABLE -> {
                swBiometria.isEnabled = true
                swBiometria.isChecked = securePrefs.isBiometricEnabled()
                tvBiometriaDesc.text = "Usa tu huella dactilar o rostro para iniciar sesión"
            }
            BiometricAuthManager.BiometricStatus.NO_HARDWARE -> {
                swBiometria.isEnabled = false
                swBiometria.isChecked = false
                tvBiometriaDesc.text = "No disponible en este dispositivo"
            }
            BiometricAuthManager.BiometricStatus.NOT_ENROLLED -> {
                swBiometria.isEnabled = false
                swBiometria.isChecked = false
                tvBiometriaDesc.text = "Configura tu huella o rostro en Ajustes"
            }
            else -> {
                swBiometria.isEnabled = false
                swBiometria.isChecked = false
                tvBiometriaDesc.text = "No disponible"
            }
        }
    }

    private fun setupListeners() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        swNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener
            
            if (isChecked) {
                verificarYSolicitarPermisoNotificaciones()
            } else {
                deshabilitarNotificaciones()
            }
        }
        
        swNotificacionFija.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener
            
            prefs.edit().putBoolean("NOTIFICACION_FIJA", isChecked).apply()
            
            if (swNotificaciones.isChecked) {
                mostrarNotificacionAhorro(isChecked)
            }
            
            Toast.makeText(
                requireContext(),
                if (isChecked) "Notificación fija activada" else "Notificación se puede descartar",
                Toast.LENGTH_SHORT
            ).show()
        }

        swTemaOscuro.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener
            
            prefs.edit().putBoolean("TEMA_OSCURO", isChecked).apply()
            aplicarTema(isChecked)
        }
        
        swBiometria.setOnClickListener {
            if (swBiometria.isChecked) {
                swBiometria.isChecked = false
                habilitarBiometria()
            } else {
                swBiometria.isChecked = true
                deshabilitarBiometria()
            }
        }
    }
    
    private fun verificarYSolicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    habilitarNotificaciones()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Permiso de notificaciones")
                        .setMessage("Balance+ necesita permiso para mostrarte alertas sobre tu ahorro.")
                        .setPositiveButton("Permitir") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Cancelar") { _, _ ->
                            swNotificaciones.isChecked = false
                        }
                        .show()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            habilitarNotificaciones()
        }
    }
    
    private fun habilitarNotificaciones() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("NOTIFICACIONES_ACTIVAS", true).apply()
        
        swNotificacionFija.isEnabled = true
        actualizarDescripcionNotificacion(true)
        
        val esFija = prefs.getBoolean("NOTIFICACION_FIJA", false)
        mostrarNotificacionAhorro(esFija)
        
        Toast.makeText(requireContext(), "Notificaciones activadas", Toast.LENGTH_SHORT).show()
    }
    
    private fun deshabilitarNotificaciones() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("NOTIFICACIONES_ACTIVAS", false).apply()
        
        swNotificacionFija.isEnabled = false
        swNotificacionFija.isChecked = false
        actualizarDescripcionNotificacion(false)
        
        notificationHelper.cancelarNotificacionAhorro()
        
        Toast.makeText(requireContext(), "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
    }
    
    private fun mostrarNotificacionAhorro(esFija: Boolean) {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val usuarioId = prefs.getInt("USER_ID", 0)
        val balanceOriginal = prefs.getString("BALANCE_ORIGINAL", "0.00")?.toDoubleOrNull() ?: 0.0
        val codigoDivisa = prefs.getString("DIVISA_CODIGO", "PEN") ?: "PEN"
        
        if (usuarioId <= 0 || balanceOriginal <= 0) return
        
        val dbHelper = AppDatabaseHelper(requireContext())
        val db = dbHelper.readableDatabase
        val transaccionDAO = TransaccionDAO(db, dbHelper)
        
        val transacciones = transaccionDAO.obtenerTransaccionesPorUsuario(usuarioId)
        
        var totalGastado = 0.0
        transacciones.forEach { t ->
            if (t.tipoCategoria.nombre == "Necesidad" || t.tipoCategoria.nombre == "Deseo") {
                totalGastado += t.transaccion.monto
            }
        }
        
        val ahorroDisponible = balanceOriginal - totalGastado
        val porcentajeAhorro = if (balanceOriginal > 0) {
            ((ahorroDisponible / balanceOriginal) * 100.0).toFloat()
        } else 0f
        
        db.close()
        
        notificationHelper.mostrarNotificacionAhorro(
            porcentajeAhorro = porcentajeAhorro,
            montoAhorro = ahorroDisponible,
            codigoDivisa = codigoDivisa,
            esPersistente = esFija
        )
    }
    
    private fun habilitarBiometria() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val email = prefs.getString("USER_EMAIL", null)
        
        if (email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Debes iniciar sesión primero", Toast.LENGTH_SHORT).show()
            return
        }
        
        biometricManager.authenticateWithDeviceCredential(
            activity = requireActivity() as FragmentActivity,
            title = "Configurar acceso biométrico",
            subtitle = "Verifica tu identidad"
        ) { result ->
            when (result) {
                is BiometricAuthManager.AuthResult.Success -> {
                    securePrefs.setBiometricEnabled(true)
                    securePrefs.saveUserEmail(email)
                    swBiometria.isChecked = true
                    Toast.makeText(requireContext(), "Acceso biométrico habilitado", Toast.LENGTH_SHORT).show()
                }
                is BiometricAuthManager.AuthResult.Cancelled -> {
                    swBiometria.isChecked = false
                }
                is BiometricAuthManager.AuthResult.Failed -> {
                    swBiometria.isChecked = false
                    Toast.makeText(requireContext(), "Verificación fallida", Toast.LENGTH_SHORT).show()
                }
                is BiometricAuthManager.AuthResult.Error -> {
                    swBiometria.isChecked = false
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun deshabilitarBiometria() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Deshabilitar biometría")
            .setMessage("¿Deseas deshabilitar el acceso con huella/rostro?")
            .setPositiveButton("Deshabilitar") { _, _ ->
                securePrefs.setBiometricEnabled(false)
                swBiometria.isChecked = false
                Toast.makeText(requireContext(), "Acceso biométrico deshabilitado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                swBiometria.isChecked = true
            }
            .setCancelable(false)
            .show()
    }

    private fun aplicarTema(modoOscuro: Boolean) {
        val nuevoModo = if (modoOscuro) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        
        if (AppCompatDelegate.getDefaultNightMode() != nuevoModo) {
            AppCompatDelegate.setDefaultNightMode(nuevoModo)
            Toast.makeText(
                requireContext(),
                if (modoOscuro) "Modo oscuro activado" else "Modo claro activado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        fun aplicarTemaGuardado(context: Context) {
            val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val modoOscuro = prefs.getBoolean("TEMA_OSCURO", false)
            
            AppCompatDelegate.setDefaultNightMode(
                if (modoOscuro) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }
}
