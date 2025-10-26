package com.app.balance

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.dao.CategoriaDAO
import com.app.balance.model.Categoria
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class CrearCategoriaActivity : AppCompatActivity() {

    private lateinit var btnRegresar: ImageButton
    private lateinit var ivIconoSeleccionado: ImageView
    private lateinit var etNombreCategoria: TextInputEditText
    private lateinit var radioGroupTipo: RadioGroup
    private lateinit var rbNecesidad: RadioButton
    private lateinit var rbDeseo: RadioButton
    private lateinit var gridIconos: GridLayout
    private lateinit var linearLayoutColores: LinearLayout
    private lateinit var btnAnadirCategoria: MaterialButton

    private var iconoSeleccionado: Int = R.drawable.ic_comida
    private var colorSeleccionado: Int = android.R.color.holo_green_light
    private var imagenPersonalizada: String? = null
    private var usuarioId: Int = 0

    private val iconosPredeterminados = listOf(
        R.drawable.ic_comida,
        R.drawable.ic_transporte,
        R.drawable.ic_casa,
        R.drawable.ic_salud,
        R.drawable.ic_educacion,
        R.drawable.ic_entretenimiento,
        R.drawable.ic_compras,
        R.drawable.ic_otros
    )

    private val coloresDisponibles = listOf(
        android.R.color.holo_green_light,
        android.R.color.holo_blue_light,
        android.R.color.holo_red_light,
        android.R.color.white,
        android.R.color.holo_orange_light,
        android.R.color.holo_purple,
        android.R.color.darker_gray
    )

    companion object {
        private const val REQUEST_GALLERY = 100
        private const val REQUEST_CAMERA = 101
        private const val REQUEST_PERMISSIONS = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crear_categoria)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        obtenerUsuarioId()
        initViews()
        setupIconos()
        setupColores()
        setupBotonAnadir()
        setupIconoClick()
        setupBotonRegresar()
    }

    private fun obtenerUsuarioId() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        usuarioId = prefs.getInt("USER_ID", 0)
    }

    private fun initViews() {
        btnRegresar = findViewById(R.id.btnRegresar)
        ivIconoSeleccionado = findViewById(R.id.ivIconoSeleccionado)
        etNombreCategoria = findViewById(R.id.etNombreCategoria)
        radioGroupTipo = findViewById(R.id.radioGroupTipo)
        rbNecesidad = findViewById(R.id.rbNecesidad)
        rbDeseo = findViewById(R.id.rbDeseo)
        gridIconos = findViewById(R.id.gridIconos)
        linearLayoutColores = findViewById(R.id.linearLayoutColores)
        btnAnadirCategoria = findViewById(R.id.btnAnadirCategoria)
    }

    private fun setupBotonRegresar() {
        btnRegresar.setOnClickListener {
            finish()
        }
    }

    private fun setupIconos() {
        iconosPredeterminados.forEach { iconoRes ->
            val imageView = ImageView(this).apply {
                val sizeInDp = 70
                val sizeInPx = (sizeInDp * resources.displayMetrics.density).toInt()

                layoutParams = GridLayout.LayoutParams().apply {
                    width = sizeInPx
                    height = sizeInPx
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(16, 16, 16, 16)
                }
                setImageResource(iconoRes)
                setPadding(4, 24, 24, 24)
                setBackgroundResource(R.drawable.fondo_circular_solido)
                elevation = 4f
                setColorFilter(getColor(android.R.color.black), PorterDuff.Mode.SRC_IN)
                scaleType = ImageView.ScaleType.CENTER_INSIDE

                setOnClickListener {
                    iconoSeleccionado = iconoRes
                    imagenPersonalizada = null
                    actualizarIconoSeleccionado()
                }
            }
            gridIconos.addView(imageView)
        }
    }
    private fun setupColores() {
        coloresDisponibles.forEach { colorRes ->
            val colorView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(70, 70).apply {
                    setMargins(12, 12, 12, 12)
                }
                setBackgroundResource(R.drawable.fondo_circular_solido)
                backgroundTintList = ColorStateList.valueOf(getColor(colorRes))

                setOnClickListener {
                    colorSeleccionado = colorRes
                    actualizarIconoSeleccionado()
                }
            }
            linearLayoutColores.addView(colorView)
        }
    }

    private fun setupIconoClick() {
        ivIconoSeleccionado.setOnClickListener {
            mostrarOpcionesImagen()
        }
    }

    private fun mostrarOpcionesImagen() {
        val opciones = arrayOf("Tomar foto", "Seleccionar de galería", "Cancelar")

        MaterialAlertDialogBuilder(this)
            .setTitle("Seleccionar imagen")
            .setItems(opciones) { dialog, which ->
                when (which) {
                    0 -> verificarPermisosYAbrirCamara()
                    1 -> abrirGaleria()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun verificarPermisosYAbrirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_PERMISSIONS
            )
        } else {
            abrirCamara()
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_GALLERY)
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_CAMERA)
        } else {
            Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamara()
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_GALLERY -> {
                    data?.data?.let { uri ->
                        imagenPersonalizada = guardarImagenEnStorage(uri)

                        ivIconoSeleccionado.background = null
                        ivIconoSeleccionado.clearColorFilter()

                        Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .into(ivIconoSeleccionado)
                    }
                }
                REQUEST_CAMERA -> {
                    val bitmap = data?.extras?.get("data") as? Bitmap
                    bitmap?.let {
                        imagenPersonalizada = guardarBitmapEnStorage(it)

                        ivIconoSeleccionado.background = null
                        ivIconoSeleccionado.clearColorFilter()

                        Glide.with(this)
                            .load(it)
                            .circleCrop()
                            .into(ivIconoSeleccionado)
                    }
                }
            }
        }
    }
    private fun guardarImagenEnStorage(uri: Uri): String {
        val fileName = "categoria_${System.currentTimeMillis()}.jpg"
        val file = File(filesDir, fileName)

        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }

            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
            }

        } catch (e: Exception) {
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return file.absolutePath
    }

    private fun guardarBitmapEnStorage(bitmap: Bitmap): String {
        val fileName = "categoria_${System.currentTimeMillis()}.jpg"
        val file = File(filesDir, fileName)

        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }

        return file.absolutePath
    }

    private fun actualizarIconoSeleccionado() {
        if (imagenPersonalizada == null) {
            ivIconoSeleccionado.setBackgroundResource(R.drawable.fondo_circular_solido)
            ivIconoSeleccionado.setImageResource(iconoSeleccionado)
            ivIconoSeleccionado.scaleType = ImageView.ScaleType.CENTER_INSIDE
            ivIconoSeleccionado.setColorFilter(getColor(colorSeleccionado), PorterDuff.Mode.SRC_IN)
        } else {

            ivIconoSeleccionado.background = null
            ivIconoSeleccionado.clearColorFilter()

            Glide.with(this)
                .load(File(imagenPersonalizada!!))
                .circleCrop()
                .into(ivIconoSeleccionado)
        }
    }
    private fun setupBotonAnadir() {
        btnAnadirCategoria.setOnClickListener {
            val nombreCategoria = etNombreCategoria.text.toString().trim()

            if (nombreCategoria.isEmpty()) {
                Toast.makeText(
                    this,
                    "Por favor ingresa un nombre para la categoría",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val tipoSeleccionado = when (radioGroupTipo.checkedRadioButtonId) {
                R.id.rbNecesidad -> 1
                R.id.rbDeseo -> 2
                else -> 1
            }

            guardarCategoriaSistema(nombreCategoria, tipoSeleccionado)
        }
    }

    private fun guardarCategoriaSistema(nombre: String, tipoId: Int) {
        val dbHelper = AppDatabaseHelper(this)
        val db = dbHelper.writableDatabase
        val categoriaSistemaDAO = CategoriaDAO(db, dbHelper)

        val iconoNombre = if (imagenPersonalizada != null) {
            "custom_${System.currentTimeMillis()}"
        } else {
            obtenerNombreIcono(iconoSeleccionado)
        }

        val categoria = Categoria(
            nombre = nombre,
            icono = iconoNombre,
            usuarioId = usuarioId,
            tipoCategoriaId = tipoId,
            rutaImagen = imagenPersonalizada,
            color = if (imagenPersonalizada == null) colorSeleccionado else null
        )

        val resultado = categoriaSistemaDAO.insertarCategoriaSistema(categoria)

        if (resultado > 0) {
            Toast.makeText(
                this,
                "Categoría '$nombre' creada exitosamente",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        } else {
            Toast.makeText(
                this,
                "Error al crear la categoría",
                Toast.LENGTH_SHORT
            ).show()
        }

        db.close()
    }

    private fun obtenerNombreIcono(iconoRes: Int): String {
        return when (iconoRes) {
            R.drawable.ic_comida -> "comida"
            R.drawable.ic_transporte -> "transporte"
            R.drawable.ic_casa -> "casa"
            R.drawable.ic_salud -> "salud"
            R.drawable.ic_educacion -> "educacion"
            R.drawable.ic_entretenimiento -> "entretenimiento"
            R.drawable.ic_compras -> "compras"
            R.drawable.ic_otros -> "otros"
            else -> "default"
        }
    }
}