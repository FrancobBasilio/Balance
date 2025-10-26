package com.app.balance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.dao.DivisaDAO
import com.app.balance.data.dao.TransaccionDAO
import com.app.balance.data.dao.UsuarioDAO
import com.app.balance.ui.BalanceUpdateListener
import com.app.balance.ui.ConfiguracionFragment
import com.app.balance.ui.DashboardFragment
import com.app.balance.ui.PerfilFragment
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class InicioActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var tvBalance: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnEditBalance: ImageButton
    private lateinit var btnMenu: ImageButton

    // Nuevo: Referencias al layout del centro
    private lateinit var layoutCentro: LinearLayout

    // Header views
    private lateinit var ivHeaderAvatar: ImageView
    private lateinit var tvHeaderName: TextView
    private lateinit var ivHeaderFlag: ImageView
    private lateinit var tvHeaderEmail: TextView

    private var codigoDivisa = ""
    private var balanceActual = 0.0

    companion object {
        private const val REQUEST_AGREGAR_GASTO = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inicio)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        initViews()
        initHeaderViews()
        setupDrawer()
        setupNavigation()
        loadBalance()
        loadHeaderData()
        setupEditBalanceButton()

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        tvBalance = findViewById(R.id.tvBalance)
        tvTotal = findViewById(R.id.tvTotal)
        btnEditBalance = findViewById(R.id.btnEditBalance)
        btnMenu = findViewById(R.id.btnMenu)
        layoutCentro = findViewById(R.id.layoutCentro) // Nueva referencia
    }

    private fun initHeaderViews() {
        val headerView = navigationView.getHeaderView(0)
        ivHeaderAvatar = headerView.findViewById(R.id.ivHeaderAvatar)
        tvHeaderName = headerView.findViewById(R.id.tvHeaderName)
        ivHeaderFlag = headerView.findViewById(R.id.ivHeaderFlag)
        tvHeaderEmail = headerView.findViewById(R.id.tvHeaderEmail)
    }

    private fun loadHeaderData() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        val nombre = prefs.getString("USER_NOMBRE", "") ?: ""
        val apellido = prefs.getString("USER_APELLIDO", "") ?: ""
        tvHeaderName.text = "$nombre $apellido"

        val email = prefs.getString("USER_EMAIL", "correo@ejemplo.com") ?: "correo@ejemplo.com"
        tvHeaderEmail.text = email

        val banderaUrl = prefs.getString("DIVISA_BANDERA", "") ?: ""
        if (banderaUrl.isNotEmpty()) {
            Glide.with(this)
                .load(banderaUrl)
                .placeholder(android.R.drawable.ic_menu_mapmode)
                .error(android.R.drawable.ic_menu_mapmode)
                .into(ivHeaderFlag)
        } else {
            ivHeaderFlag.setImageResource(android.R.drawable.ic_menu_mapmode)
        }

        val fotoPerfilPath = prefs.getString("FOTO_PERFIL_PATH", null)
        if (fotoPerfilPath != null) {
            val file = File(fotoPerfilPath)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .circleCrop()
                    .placeholder(R.drawable.ic_persona)
                    .error(R.drawable.ic_persona)
                    .into(ivHeaderAvatar)
            } else {
                ivHeaderAvatar.setImageResource(R.drawable.ic_persona)
            }
        } else {
            ivHeaderAvatar.setImageResource(R.drawable.ic_persona)
        }
    }

    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(navigationView)
        }
    }

    private fun setupNavigation() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inicio -> {
                    loadFragment(DashboardFragment())
                    mostrarHeaderBalance(true) // Mostrar balance en Dashboard
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_perfil -> {
                    loadFragment(PerfilFragment())
                    mostrarHeaderBalance(false) // Ocultar balance en Perfil
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_config -> {
                    loadFragment(ConfiguracionFragment())
                    mostrarHeaderBalance(false) // Ocultar balance en Configuración
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_log_out -> {
                    mostrarDialogoCerrarSesion()
                    true
                }
                else -> false
            }
        }
    }

    // NUEVA FUNCIÓN: Controlar visibilidad del balance y botón de actualizar
    private fun mostrarHeaderBalance(mostrar: Boolean) {
        if (mostrar) {
            layoutCentro.visibility = View.VISIBLE
            btnEditBalance.visibility = View.VISIBLE
        } else {
            layoutCentro.visibility = View.GONE
            btnEditBalance.visibility = View.GONE
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun loadBalance() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        codigoDivisa = prefs.getString("DIVISA_CODIGO", "PEN") ?: "PEN"
        val balanceString = prefs.getString("BALANCE_MONTO", "0.00") ?: "0.00"

        try {
            balanceActual = balanceString.toDouble()
        } catch (e: Exception) {
            balanceActual = 0.0
        }

        actualizarVistasBalance()
    }

    private fun actualizarVistasBalance() {
        tvTotal.text = "Ingresos"
        tvBalance.text = "$codigoDivisa ${String.format("%.2f", balanceActual)}"
    }

    private fun setupEditBalanceButton() {
        btnEditBalance.setOnClickListener {
            mostrarDialogoActualizarBalance()
        }
    }

    private fun mostrarDialogoActualizarBalance() {
        val dialogView = layoutInflater.inflate(R.layout.dialogo_actualizar_balance, null)

        val tvBalanceActual = dialogView.findViewById<TextView>(R.id.tvBalanceActual)
        val tietEditarBalance = dialogView.findViewById<TextInputEditText>(R.id.tietEditarBalance)
        val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btnCancelar)
        val btnActualizar = dialogView.findViewById<MaterialButton>(R.id.btnActualizarBalance)

        tvBalanceActual.text = "$codigoDivisa ${String.format("%.2f", balanceActual)}"
        tietEditarBalance.hint = "Ingresa nuevo balance"

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .show()

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnActualizar.setOnClickListener {
            val nuevoBalanceText = tietEditarBalance.text.toString().trim()

            if (nuevoBalanceText.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa un valor", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevoBalanceIngresado = nuevoBalanceText.toDoubleOrNull()
            if (nuevoBalanceIngresado == null || nuevoBalanceIngresado <= 0) {
                Toast.makeText(this, "Ingresa un valor válido mayor a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val userId = prefs.getInt("USER_ID", 0)

            val dbHelper = AppDatabaseHelper(this)
            val db = dbHelper.readableDatabase
            val transaccionDAO = TransaccionDAO(db, dbHelper)

            val transacciones = transaccionDAO.obtenerTransaccionesPorUsuario(userId)
            var totalGastado = 0.0

            transacciones.forEach { transaccion ->
                totalGastado += transaccion.transaccion.monto
            }

            if (nuevoBalanceIngresado < totalGastado) {
                Toast.makeText(
                    this,
                    "El nuevo balance debe ser mayor o igual a tus gastos totales ($codigoDivisa ${String.format("%.2f", totalGastado)})",
                    Toast.LENGTH_LONG
                ).show()
                db.close()
                return@setOnClickListener
            }

            val balanceReal = nuevoBalanceIngresado - totalGastado

            prefs.edit()
                .putString("BALANCE_MONTO", balanceReal.toString())
                .putString("BALANCE_ORIGINAL", nuevoBalanceIngresado.toString())
                .apply()

            balanceActual = balanceReal
            actualizarVistasBalance()

            val usuarioDAO = UsuarioDAO(db, dbHelper)
            usuarioDAO.actualizarMontoTotal(userId, balanceReal)
            db.close()

            notificarCambioBalance(balanceReal, codigoDivisa)

            if (totalGastado > 0) {
                Toast.makeText(
                    this,
                    "Balance actualizado: $codigoDivisa ${String.format("%.2f", nuevoBalanceIngresado)}\n" +
                            "Gastos: $codigoDivisa ${String.format("%.2f", totalGastado)}\n" +
                            "Disponible: $codigoDivisa ${String.format("%.2f", balanceReal)}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Balance actualizado: $codigoDivisa ${String.format("%.2f", balanceReal)}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            dialog.dismiss()
        }
    }

    private fun notificarCambioBalance(nuevoBalance: Double, codigoDivisa: String) {
        val fragmentActual = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

        if (fragmentActual is BalanceUpdateListener) {
            fragmentActual.onBalanceUpdated(nuevoBalance, codigoDivisa)
        }

        if (fragmentActual is DashboardFragment) {
            fragmentActual.childFragmentManager.fragments.forEach { childFragment ->
                if (childFragment is BalanceUpdateListener) {
                    childFragment.onBalanceUpdated(nuevoBalance, codigoDivisa)
                }
            }
        }
    }

    fun recargarBalance() {
        loadBalance()
        notificarCambioBalance(balanceActual, codigoDivisa)
    }

    fun recargarHeader() {
        loadHeaderData()
    }

    private fun verificarYCargarDivisaDesdeDB() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val codigoDivisaActual = prefs.getString("DIVISA_CODIGO", "")

        if (codigoDivisaActual.isNullOrEmpty() || codigoDivisaActual == "PEN") {
            val userId = prefs.getInt("USER_ID", 0)

            if (userId > 0) {
                val dbHelper = AppDatabaseHelper(this)
                val db = dbHelper.readableDatabase
                val usuarioDAO = UsuarioDAO(db, dbHelper)
                val divisaDAO = DivisaDAO(db, dbHelper)

                val usuario = usuarioDAO.obtenerUsuarioPorId(userId)

                if (usuario != null) {
                    val divisa = divisaDAO.obtenerDivisaPorId(usuario.divisaId)

                    if (divisa != null) {
                        prefs.edit()
                            .putInt("DIVISA_ID", divisa.id)
                            .putString("DIVISA_CODIGO", divisa.codigo)
                            .putString("DIVISA_NOMBRE", divisa.nombre)
                            .putString("DIVISA_BANDERA", divisa.bandera)
                            .apply()

                        codigoDivisa = divisa.codigo

                        Log.d("InicioActivity", "Divisa restaurada desde BD: ${divisa.codigo}")
                    }
                }

                db.close()
            }
        } else {
            codigoDivisa = codigoDivisaActual
        }
    }

    override fun onResume() {
        super.onResume()
        verificarYCargarDivisaDesdeDB()
        recargarBalance()
        loadHeaderData()
    }

    private fun mostrarDialogoCerrarSesion() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Cerrar sesión") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        val esPrimeraVez = prefs.getBoolean("ES_PRIMERA_VEZ", true)

        prefs.edit().clear().apply()

        prefs.edit()
            .putBoolean("ES_PRIMERA_VEZ", esPrimeraVez)
            .putBoolean("SESION_ACTIVA", false)
            .apply()

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}