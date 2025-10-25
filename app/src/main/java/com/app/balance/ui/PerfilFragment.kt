package com.app.balance.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.app.balance.InicioActivity
import com.app.balance.R
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PerfilFragment : Fragment(R.layout.fragment_perfil) {

    private lateinit var ivFotoPerfil: ImageView
    private lateinit var btnCambiarFoto: MaterialButton
    private lateinit var tvNombreCompleto: TextView
    private lateinit var tvNombrePais: TextView
    private lateinit var ivBanderaPais: ImageView
    private lateinit var tvAhorroDisponible: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvCelular: TextView

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private val PERMISSION_REQUEST_CODE = 100

    private var fotoUri: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        cargarDatosUsuario()
        setupCambiarFoto()
    }

    private fun initViews(view: View) {
        ivFotoPerfil = view.findViewById(R.id.ivFotoPerfil)
        btnCambiarFoto = view.findViewById(R.id.btnCambiarFoto)
        tvNombreCompleto = view.findViewById(R.id.tvNombreCompleto)
        tvNombrePais = view.findViewById(R.id.tvNombrePais)
        ivBanderaPais = view.findViewById(R.id.ivBanderaPais)
        tvAhorroDisponible = view.findViewById(R.id.tvAhorroDisponible)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvCelular = view.findViewById(R.id.tvCelular)
    }

    private fun cargarDatosUsuario() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Nombre completo
        val nombre = prefs.getString("USER_NOMBRE", "") ?: ""
        val apellido = prefs.getString("USER_APELLIDO", "") ?: ""
        tvNombreCompleto.text = "$nombre $apellido"

        // País
        val nombrePais = prefs.getString("DIVISA_NOMBRE", "Perú") ?: "Perú"
        val banderaUrl = prefs.getString("DIVISA_BANDERA", "") ?: ""
        tvNombrePais.text = nombrePais

        // Cargar bandera usando Glide (si tienes la URL)
        if (banderaUrl.isNotEmpty()) {
            Glide.with(this)
                .load(banderaUrl)
                .placeholder(android.R.drawable.ic_menu_mapmode)
                .error(android.R.drawable.ic_menu_mapmode)
                .into(ivBanderaPais)
        } else {
            ivBanderaPais.setImageResource(android.R.drawable.ic_menu_mapmode)
        }

        // Ahorro disponible
        val balanceMonto = prefs.getString("BALANCE_MONTO", "0.00") ?: "0.00"
        val codigoDivisa = prefs.getString("DIVISA_CODIGO", "PEN") ?: "PEN"
        tvAhorroDisponible.text = "$codigoDivisa ${String.format("%.2f", balanceMonto.toDoubleOrNull() ?: 0.0)}"

        // Email
        val email = prefs.getString("USER_EMAIL", "correo@ejemplo.com") ?: "correo@ejemplo.com"
        tvEmail.text = email

        // Celular
        val celular = prefs.getString("USER_CELULAR", "") ?: ""
        tvCelular.text = celular

        // Cargar foto de perfil guardada (si existe)
        val fotoPerfilPath = prefs.getString("FOTO_PERFIL_PATH", null)
        if (fotoPerfilPath != null) {
            val file = File(fotoPerfilPath)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .circleCrop()
                    .into(ivFotoPerfil)
            }
        }
    }

    private fun setupCambiarFoto() {
        btnCambiarFoto.setOnClickListener {
            mostrarOpcionesFoto()
        }
    }

    private fun mostrarOpcionesFoto() {
        val opciones = arrayOf("Tomar foto", "Elegir de galería", "Cancelar")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cambiar foto de perfil")
            .setItems(opciones) { dialog, which ->
                when (which) {
                    0 -> verificarPermisosYTomarFoto()
                    1 -> abrirGaleria()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun verificarPermisosYTomarFoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                abrirCamara()
            }
        } else {
            abrirCamara()
        }
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Crear archivo temporal para guardar la foto
        val photoFile = crearArchivoTemporal()
        if (photoFile != null) {
            fotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri)
            startActivityForResult(intent, TAKE_PHOTO_REQUEST)
        } else {
            Toast.makeText(requireContext(), "Error al crear archivo para la foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun crearArchivoTemporal(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile("FOTO_PERFIL_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    // Foto desde galería
                    data?.data?.let { uri ->
                        guardarYMostrarFoto(uri)
                    }
                }
                TAKE_PHOTO_REQUEST -> {
                    // Foto desde cámara
                    fotoUri?.let { uri ->
                        guardarYMostrarFoto(uri)
                    }
                }
            }
        }
    }

    private fun guardarYMostrarFoto(uri: Uri) {
        try {
            // Copiar imagen a almacenamiento interno de la app
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileName = "perfil_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, fileName)

            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Guardar ruta en SharedPreferences
            val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("FOTO_PERFIL_PATH", file.absolutePath)
                .apply()

            // Mostrar foto en ImageView
            Glide.with(this)
                .load(file)
                .circleCrop()
                .into(ivFotoPerfil)

            // ← NUEVO: Actualizar el header del menú
            (requireActivity() as? InicioActivity)?.recargarHeader()

            Toast.makeText(requireContext(), "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al guardar la foto", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamara()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permiso de cámara denegado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}