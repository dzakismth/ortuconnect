package com.example.mobiles_tktech

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import com.example.mobiles_tktech.login.SessionManager
import com.example.mobiles_tktech.navigasi.NavigasiCard

class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(applicationContext)

        // PERUBAHAN: Validasi session sebelum auto-login
        if (sessionManager.isLoggedIn) {
            validateSessionAndLogin()
        } else {
            showLoginScreen()
        }
    }

    // FUNGSI BARU: Validasi apakah akun masih ada di database
    private fun validateSessionAndLogin() {
        val username = sessionManager.username ?: ""

        // Jika username kosong, langsung logout dan tampilkan login
        if (username.isEmpty()) {
            sessionManager.logoutUser()
            showLoginScreen()
            return
        }

        // URL endpoint untuk validasi session
        val url = "http://ortuconnect.atwebpages.com/api/validate_session.php"

        val params = JSONObject().apply {
            put("username", username)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, params,
            { response ->
                try {
                    val isValid = response.optBoolean("valid", false)

                    if (isValid) {
                        // Session valid, lanjut ke dashboard
                        startDashboard()
                    } else {
                        // Session tidak valid (akun dihapus), logout dan tampilkan login
                        val message = response.optString("message", "Akun tidak ditemukan")
                        sessionManager.logoutUser()
                        runOnUiThread {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                        showLoginScreen()
                    }
                } catch (e: Exception) {
                    Log.e("SessionValidation", "Error parsing response: ${e.message}", e)
                    handleValidationError()
                }
            },
            { error ->
                Log.e("SessionValidation", "Network error: ${error.message}", error)
                handleValidationError()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    // FUNGSI BARU: Handle error saat validasi
    private fun handleValidationError() {
        // Jika error koneksi, beri kesempatan user untuk tetap masuk
        // atau logout dan tampilkan login screen
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Gagal Validasi Session")
                .setMessage("Tidak dapat memverifikasi akun. Lanjutkan atau login ulang?")
                .setPositiveButton("Lanjutkan") { _, _ ->
                    startDashboard()
                }
                .setNegativeButton("Login Ulang") { _, _ ->
                    sessionManager.logoutUser()
                    showLoginScreen()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun showLoginScreen() {
        setContent {
            LoginScreen(sessionManager)
        }
    }

    private fun startDashboard() {
        startActivity(Intent(this, NavigasiCard::class.java))
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(sessionManager: SessionManager) {
    val context = LocalContext.current
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                )
            )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_ortuconnect),
                    contentDescription = "Logo OrtuConnect",
                    modifier = Modifier.size(120.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (username.isEmpty() || password.isEmpty()) {
                            Toast.makeText(context, "Isi semua kolom", Toast.LENGTH_SHORT).show()
                        } else {
                            isLoading = true
                            val url = "http://ortuconnect.atwebpages.com/api/login.php"

                            try {
                                val params = JSONObject().apply {
                                    put("username", username)
                                    put("password", password)
                                }

                                val request = JsonObjectRequest(
                                    Request.Method.POST, url, params,
                                    { response ->
                                        isLoading = false
                                        try {
                                            val success = response.optBoolean("success", false)
                                            val message = response.optString("message", "Terjadi kesalahan server atau data tidak lengkap (E1).")

                                            if (success) {
                                                val user = response.optJSONObject("user")
                                                val usernameResp = user?.optString("username", username) ?: username
                                                val token = user?.optString("token", "") ?: ""

                                                // Simpan session menggunakan SessionManager Java
                                                sessionManager.createLoginSession(true, token)
                                                sessionManager.saveUsername(usernameResp)

                                                Toast.makeText(context, "Login berhasil", Toast.LENGTH_SHORT).show()

                                                context.startActivity(Intent(context, NavigasiCard::class.java))
                                                (context as? ComponentActivity)?.finish()
                                            } else {
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("LoginFix", "Exception di Volley Success: ${e.message}", e)
                                            Toast.makeText(context, "Data respons tidak valid. Tetap di login (E2).", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    { error ->
                                        isLoading = false
                                        Log.e("LoginFix", "Volley Error: ${error.message}", error)
                                        Toast.makeText(context, "Gagal koneksi ke server. Coba lagi (E3).", Toast.LENGTH_LONG).show()
                                    }
                                )

                                Volley.newRequestQueue(context).add(request)
                            } catch (e: Exception) {
                                isLoading = false
                                Log.e("LoginFix", "Exception saat membuat request: ${e.message}", e)
                                Toast.makeText(context, "Kesalahan internal aplikasi (E4).", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Masuk", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}