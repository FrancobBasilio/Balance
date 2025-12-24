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
import androidx.viewpager2.widget.ViewPager2
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.dao.DivisaDAO
import com.app.balance.data.dao.TransaccionDAO
import com.app.balance.data.dao.UsuarioDAO
import com.app.balance.ui.AcercaFragment
import com.app.balance.ui.BalanceUpdateListener
import com.app.balance.ui.ConfiguracionFragment
import com.app.balance.ui.DashboardFragment
import com.app.balance.ui.PerfilFragment
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.tabs.TabLayout
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView

import java.io.File

class InicioActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var tvBalance: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnEditBalance: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var layoutCentro: LinearLayout

    private lateinit var ivHeaderAvatar: ImageView
    private lateinit var tvHeaderName: TextView
    private lateinit var ivHeaderFlag: ImageView
    private lateinit var tvHeaderEmail: TextView
    private lateinit var ivHeaderLogo: TextView

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
        layoutCentro = findViewById(R.id.layoutCentro)
        ivHeaderLogo = findViewById(R.id.ivHeaderLogo)
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
                    mostrarHeaderBalance(true)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_perfil -> {
                    loadFragment(PerfilFragment())
                    mostrarHeaderBalance(false)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_config -> {
                    loadFragment(ConfiguracionFragment())
                    mostrarHeaderBalance(false)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_about -> {
                    loadFragment(AcercaFragment())
                    mostrarHeaderBalance(false)
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

    private fun mostrarHeaderBalance(mostrar: Boolean) {
        if (mostrar) {
            layoutCentro.visibility = View.VISIBLE
            btnEditBalance.visibility = View.VISIBLE
            ivHeaderLogo.visibility = View.GONE
        } else {
            layoutCentro.visibility = View.GONE
            btnEditBalance.visibility = View.GONE
            ivHeaderLogo.visibility = View.VISIBLE
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
                cerrarSesion() // <-- Llama a la función de abajo
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    // --- LÓGICA para CIERRE DE SESIÓN y NO BORRA el historial del tour.

    private fun cerrarSesion() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // 1. Antes de borrar, se leen los flags que se quieren mantener
        val esPrimeraVez = prefs.getBoolean("ES_PRIMERA_VEZ", true)
        val tourHecho = prefs.getBoolean("TUTORIAL_DASHBOARD_DONE", false)

        // 2. Se limipan los datos (ID de usuario, email, etc.)
        prefs.edit().clear().apply()

        // 3. Se guadan flags importantes
        prefs.edit()
            .putBoolean("ES_PRIMERA_VEZ", esPrimeraVez)
            .putBoolean("TUTORIAL_DASHBOARD_DONE", tourHecho) // VALIDACIÓN
            .putBoolean("SESION_ACTIVA", false)
            .apply()

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    //  TOUR INTERACTIVO (usando 'TabLayout' y 'ViewPager2')
    // =====================================================

    // Variable para no mostrar el diálogo de tour más de una vez por sesión
    private var pregunteEstaVez = false

    // --- Definimos colores ---
    private val GOLD = 0xFFF1B623.toInt()
    private val GOLD_SOFT = 0xFFFFE29A.toInt()
    private val TEXT = 0xFF202124.toInt()

    // Estilos a los círculos del tour.

    private fun TapTarget.estilo(): TapTarget = this
        .outerCircleColorInt(GOLD)
        .targetCircleColorInt(GOLD_SOFT)
        .titleTextColorInt(TEXT)
        .descriptionTextColorInt(TEXT)
        .transparentTarget(true)
        .drawShadow(true)
        .cancelable(false) // El usuario DEBE tocar el círculo

   // Busca un ID DENTRO del DashboardFragment (Fragment Padre)
    //Este ID ayuda a encontrar el TabLayout y el ViewPager.

    private fun vistaEnDashboardPadre(id: Int): View? {
        val frag =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? com.app.balance.ui.DashboardFragment
        if (frag?.view == null) {
            Log.e("Tour", "DashboardFragment (Padre) no encontrado o su vista es nula.")
            return null
        }
        return frag.view?.findViewById(id)
    }

    // Busca un ID DENTRO del FRAGMENTO HIJO que está actualmente visible en el ViewPager.
    // Se usa encontrar "fabCrearCategoria"
    private fun vistaEnHijoDelViewPager(id: Int): View? {
        val dashFrag =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? com.app.balance.ui.DashboardFragment
        if (dashFrag?.view == null) {
            Log.e("Tour", "vistaEnHijo: DashboardFragment (Padre) no encontrado.")
            return null
        }

        // Usamos el ID de  DashboardFragment
        val viewPager = dashFrag.view?.findViewById<ViewPager2>(R.id.viewPager)
        if (viewPager == null) {
            Log.e("Tour", "vistaEnHijo: ViewPager2 (R.id.viewPager) no encontrado.")
            return null
        }

        // El ViewPager2 crea fragments con un etiqueta "f" + la posición
        val currentFrag =
            dashFrag.childFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
        if (currentFrag?.view == null) {
            Log.e(
                "Tour",
                "vistaEnHijo: Fragmento hijo no encontrado (Tag: f${viewPager.currentItem})."
            )
            return null
        }

        return currentFrag.view?.findViewById(id)
    }


    // ¡CONDICIÓN CLAVE!
     // Aquí se decide si mostramos el tour. Solo lo hace si 'TUTORIAL_DASHBOARD_DONE' es 'false'.

    private fun esUsuarioNuevo(prefs: android.content.SharedPreferences): Boolean {
        val tourHecho = prefs.getBoolean("TUTORIAL_DASHBOARD_DONE", false)
        return !tourHecho
    }

     // Revisa si estamos en la pantalla de Dashboard.
     private fun estamosEnDashboard(): Boolean {
        val frag = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        return (frag is com.app.balance.ui.DashboardFragment)
    }

    // Revisa todas las condiciones antes de mostrar el diálogo.
    private fun mostrarTourSiUsuarioNuevo() {
        if (pregunteEstaVez) return
        if (!estamosEnDashboard()) return

        val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        if (!esUsuarioNuevo(prefs)) {
            Log.d("Tour", "El usuario ya vio el tour.")
            return
        }

        pregunteEstaVez = true
        Log.d("Tour", "Es usuario nuevo. Mostrando diálogo de tour.")

        // ¡¡CAMBIO AQUÍ!! Usamos el nuevo estilo ".Solid"
        MaterialAlertDialogBuilder(this, R.style.TourAlertDialog_Solid)
            .setTitle("¿Quieres iniciar el tour?")
            .setMessage("¡Te mostramos lo esencial de Balance+!")
            .setPositiveButton("Iniciar") { _, _ ->
                Log.d("Tour", "Usuario aceptó el tour.")
                prefs.edit().putBoolean("ES_PRIMERA_VEZ", false).apply()

                // Esperamos un poco para que el DashboardFragment y sus hijos carguen
                drawerLayout.postDelayed({
                    iniciarTourPaso1_Balance()
                }, 700) // Un poco más de tiempo por si acaso
            }
            .setNegativeButton("No, gracias") { _, _ ->
                Log.d("Tour", "Usuario rechazó el tour. Marcando como 'hecho'.")
                prefs.edit()
                    .putBoolean("ES_PRIMERA_VEZ", false)
                    .putBoolean("TUTORIAL_DASHBOARD_DONE", true)
                    .apply()
            }
            .show()
    }

    // ---------- EL FLUJO DEL TOUR  ----------
    //PASO 1: Mostrar el botón de Editar Balance (Header)

    private fun iniciarTourPaso1_Balance() {
        val vBalance = findViewById<View>(R.id.btnEditBalance)
        if (vBalance == null) {
            Log.e("Tour", "Paso 1: ¡ERROR! No se encontró 'R.id.btnEditBalance'.")
            finalizarTour(marcarComoHecho = false)
            return
        }

        TapTargetView.showFor(this,
            TapTarget.forView(
                vBalance,
                "1. Tu Balance",
                "Toca aquí para actualizar el monto total de tu balance" ).estilo(),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    mostrarTourPaso2_TabGastos() // Siguiente paso
                }

                override fun onTargetLongClick(view: TapTargetView) {
                    onTargetClick(view)
                }

                override fun onTargetCancel(view: TapTargetView) {
                    finalizarTour(marcarComoHecho = false)
                }
            }
        )
    }
    //PASO 2: Mostrar la Pestaña "Gastos"
    private fun mostrarTourPaso2_TabGastos() {
        val tabLayout = vistaEnDashboardPadre(R.id.tabLayout) as? TabLayout
        if (tabLayout == null) {
            Log.e("Tour", "Paso 2: ¡ERROR! No se encontró el TabLayout (R.id.tabLayout).")
            finalizarTour(marcarComoHecho = true) // Vio el paso 1, así que marcamos
            return
        }

        // "Gastos" es la primera pestaña (índice 0)
        val vTabGastos = tabLayout.getTabAt(0)?.view
        if (vTabGastos == null) {
            Log.e("Tour", "Paso 2: ¡ERROR! No se encontró la pestaña en el índice 0.")
            finalizarTour(marcarComoHecho = true)
            return
        }

        TapTargetView.showFor(this,
            TapTarget.forView(
                vTabGastos,
                "2. Tus gastos ",
                "Aquí verás el historial de tus gastos/transacciones."
            ).estilo(),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    // Navegamos a esa pestaña
                    tabLayout.getTabAt(0)?.select()
                    // Esperamos que el ViewPager cambie de fragment
                    drawerLayout.postDelayed({
                        mostrarTourPaso3_TabDashboard()
                    }, 400)
                }

                override fun onTargetLongClick(view: TapTargetView) {
                    onTargetClick(view)
                }

                override fun onTargetCancel(view: TapTargetView) {
                    finalizarTour(marcarComoHecho = true)
                }
            }
        )
    }

   //PASO 3: Mostrar la Pestaña "Dashboard"
    private fun mostrarTourPaso3_TabDashboard() {
        val tabLayout = vistaEnDashboardPadre(R.id.tabLayout) as? TabLayout
        if (tabLayout == null) {
            Log.e("Tour", "Paso 3: ¡ERROR! No se encontró el TabLayout.")
            finalizarTour(marcarComoHecho = true)
            return
        }

        // "Dashboard" es la segunda pestaña (índice 1)
        val vTabDashboard = tabLayout.getTabAt(1)?.view
        if (vTabDashboard == null) {
            Log.e("Tour", "Paso 3: ¡ERROR! No se encontró la pestaña en el índice 1.")
            finalizarTour(marcarComoHecho = true)
            return
        }

        TapTargetView.showFor(this,
            TapTarget.forView(
                vTabDashboard,
                "3. Tu Dashboard",
                "Toca aquí para tu resumen financero."
            ).estilo(),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    // ¡Navegamos!
                    tabLayout.getTabAt(1)?.select()
                    drawerLayout.postDelayed({
                        mostrarTourPaso4_BotonCategoria()
                    }, 400)
                }

                override fun onTargetLongClick(view: TapTargetView) {
                    onTargetClick(view)
                }

                override fun onTargetCancel(view: TapTargetView) {
                    finalizarTour(marcarComoHecho = true)
                }
            }
        )
    }

    //PASO 4: Mostrar el Botón de "Crear Categoría" (FAB)

    private fun mostrarTourPaso4_BotonCategoria() {
        val vBotonCategoria = vistaEnHijoDelViewPager(R.id.fabCrearCategoria)

        if (vBotonCategoria == null) {
            Log.e(
                "Tour",
                "Paso 4: ¡AVISO! No se encontró 'R.id.ID_DE_TU_BOTON_CATEGORIA_FAB'. Saltando al paso 5."
            )
            mostrarTourPaso5_TabBalance() // Si no lo encuentra, salta al último paso
            return
        }

        TapTargetView.showFor(this,
            TapTarget.forView(
                vBotonCategoria,
                "4. Agregar categorías / transacciones",
                "Toca aquí para añadir categorías y transacciones."
            ).estilo(),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    mostrarTourPaso5_TabBalance() // Siguiente paso
                }

                override fun onTargetLongClick(view: TapTargetView) {
                    onTargetClick(view)
                }

                override fun onTargetCancel(view: TapTargetView) {
                    finalizarTour(marcarComoHecho = true)
                }
            }
        )
    }

    //PASO 5: Mostrar la Pestaña "Balance" (ahorro)
    private fun mostrarTourPaso5_TabBalance() {
        val tabLayout = vistaEnDashboardPadre(R.id.tabLayout) as? TabLayout
        if (tabLayout == null) {
            Log.e("Tour", "Paso 5: ¡ERROR! No se encontró el TabLayout.")
            finalizarTour(marcarComoHecho = true)
            return
        }

        // "Balance" (Ahorro) es la tercera pestaña (índice 2)
        val vTabBalance = tabLayout.getTabAt(2)?.view
        if (vTabBalance == null) {
            Log.e("Tour", "Paso 5: ¡ERROR! No se encontró la pestaña en el índice 2.")
            finalizarTour(marcarComoHecho = true)
            return
        }

        TapTargetView.showFor(this,
            TapTarget.forView(vTabBalance, "5. Tu Balance", "Toca aquí para ver tu control financiero.")
                .estilo(),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    // ¡Navegamos!
                    tabLayout.getTabAt(2)?.select()
                    drawerLayout.postDelayed({
                        finalizarTour(marcarComoHecho = true)
                    }, 400)
                }

                override fun onTargetLongClick(view: TapTargetView) {
                    onTargetClick(view)
                }

                override fun onTargetCancel(view: TapTargetView) {
                    finalizarTour(marcarComoHecho = true)
                }
            }
        )
    }

    //FINAL: Marca el tour como completado y muestra el mensaje final.
    private fun finalizarTour(marcarComoHecho: Boolean) {
        if (marcarComoHecho) {
            Log.d("Tour", "Tour finalizado y guardado.")
            val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            prefs.edit().putBoolean("TUTORIAL_DASHBOARD_DONE", true).apply()

            // se agrega un estilo
            MaterialAlertDialogBuilder(this@InicioActivity, R.style.TourAlertDialog_Solid)
                .setTitle("¡Listo!")
                .setMessage("Ya conoces lo esencial de BALANCE+   Disfruta la app.")
                .setPositiveButton("Aceptar", null)
                .show()
        } else {
            Log.e("Tour", "Tour finalizado por un error, no se marcará como 'hecho'.")
        }
    }

    //Se llama cuando la Activity es visible.

    override fun onPostResume() {
        super.onPostResume()
        mostrarTourSiUsuarioNuevo()
    }

}