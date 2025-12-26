package com.app.balance

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.exifinterface.media.ExifInterface
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.dao.CategoriaDAO
import com.app.balance.model.Categoria
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    
    // Para captura de cámara con alta calidad
    private var fotoUri: Uri? = null
    private var fotoPath: String? = null

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
        android.R.color.holo_orange_light,
        android.R.color.darker_gray,
        android.R.color.holo_blue_dark,
        android.R.color.holo_green_dark,
        android.R.color.holo_red_dark,
        android.R.color.holo_orange_dark,
        android.R.color.holo_purple,
        R.color.colorCyan,
        R.color.colorPink,
        R.color.colorLime,
        R.color.colorTeal
    )

    companion object {
        private const val REQUEST_GALLERY = 100
        private const val REQUEST_CAMERA = 101
        private const val REQUEST_PERMISSIONS = 102
        private const val MAX_IMAGE_SIZE = 1024
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

    private var colorViewSeleccionado: View? = null

    private fun dpToPx(dp: Int): Int {
        val scale = resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    private fun setupColores() {
        linearLayoutColores.removeAllViews()
        val sizePx = dpToPx(32)
        val marginPx = dpToPx(5)
        val strokeNormal = dpToPx(1)
        val strokeSeleccionado = dpToPx(2)

        coloresDisponibles.forEach { colorRes ->
            val colorView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    setMargins(marginPx, marginPx, marginPx, marginPx)
                }
                isClickable = true
                isFocusable = true
            }

            val colorInt = try {
                ContextCompat.getColor(this, colorRes)
            } catch (e: Exception) {
                colorRes
            }

            val gd = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(colorInt)
                setStroke(strokeNormal, android.graphics.Color.BLACK)
            }

            colorView.background = gd

            colorView.setOnClickListener {
                colorViewSeleccionado?.background?.let { prevBg ->
                    if (prevBg is android.graphics.drawable.GradientDrawable) {
                        prevBg.setStroke(strokeNormal, android.graphics.Color.BLACK)
                    }
                }

                (colorView.background as? android.graphics.drawable.GradientDrawable)
                    ?.setStroke(strokeSeleccionado, android.graphics.Color.BLACK)

                colorViewSeleccionado = colorView
                colorSeleccionado = colorRes
                actualizarIconoSeleccionado()
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
        val opciones = arrayOf("Tomar foto", "Elegir de galería", "Cancelar")

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
            val archivoFoto = crearArchivoImagen()
            archivoFoto?.let {
                fotoPath = it.absolutePath
                fotoUri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri)
                startActivityForResult(intent, REQUEST_CAMERA)
            }
        } else {
            Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun crearArchivoImagen(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val nombreArchivo = "IMG_${timeStamp}"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(nombreArchivo, ".jpg", storageDir)
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
                        mostrarImagenSeleccionada()
                    }
                }
                REQUEST_CAMERA -> {
                    fotoPath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            imagenPersonalizada = optimizarYGuardarImagen(file)
                            mostrarImagenSeleccionada()
                        }
                    }
                }
            }
        }
    }
    
    private fun mostrarImagenSeleccionada() {
        imagenPersonalizada?.let { path ->
            ivIconoSeleccionado.background = null
            ivIconoSeleccionado.clearColorFilter()
            
            Glide.with(this)
                .load(File(path))
                .circleCrop()
                .into(ivIconoSeleccionado)
        }
    }
    
    /**
     * Obtiene la rotación necesaria basada en EXIF
     */
    private fun obtenerRotacionExif(path: String): Int {
        return try {
            val exif = ExifInterface(path)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Rota un bitmap según el ángulo dado
     */
    private fun rotarBitmap(bitmap: Bitmap, grados: Int): Bitmap {
        if (grados == 0) return bitmap
        
        val matrix = Matrix()
        matrix.postRotate(grados.toFloat())
        
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        
        return rotated
    }
    
    private fun optimizarYGuardarImagen(archivoOriginal: File): String {
        val fileName = "categoria_${System.currentTimeMillis()}.jpg"
        val archivoFinal = File(filesDir, fileName)
        
        try {
            // Obtener rotación EXIF antes de procesar
            val rotacion = obtenerRotacionExif(archivoOriginal.absolutePath)
            
            // Decodificar con opciones para obtener dimensiones
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(archivoOriginal.absolutePath, options)
            
            // Calcular factor de escala
            val scaleFactor = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
            
            // Decodificar con el factor de escala
            options.apply {
                inJustDecodeBounds = false
                inSampleSize = scaleFactor
            }
            
            var bitmap = BitmapFactory.decodeFile(archivoOriginal.absolutePath, options)
            
            // Rotar si es necesario
            bitmap = rotarBitmap(bitmap, rotacion)
            
            // Guardar con alta calidad (95%)
            FileOutputStream(archivoFinal).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            bitmap.recycle()
            
            // Eliminar archivo temporal
            archivoOriginal.delete()
            
        } catch (e: Exception) {
            e.printStackTrace()
            archivoOriginal.copyTo(archivoFinal, overwrite = true)
            archivoOriginal.delete()
        }
        
        return archivoFinal.absolutePath
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun guardarImagenEnStorage(uri: Uri): String {
        val fileName = "categoria_${System.currentTimeMillis()}.jpg"
        val file = File(filesDir, fileName)

        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            
            // Escalar si es necesario
            val scaledBitmap = escalarBitmapSiNecesario(bitmap)

            file.outputStream().use { output ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
            }
            
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()

        } catch (e: Exception) {
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return file.absolutePath
    }
    
    private fun escalarBitmapSiNecesario(bitmap: Bitmap): Bitmap {
        val maxDimension = MAX_IMAGE_SIZE
        
        if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) {
            return bitmap
        }
        
        val ratio = minOf(
            maxDimension.toFloat() / bitmap.width,
            maxDimension.toFloat() / bitmap.height
        )
        
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
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
