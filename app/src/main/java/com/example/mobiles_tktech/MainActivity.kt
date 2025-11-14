package com.example.mobiles_tktech

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.android.volley.Response
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

        val isLoggedIn = sessionManager.isLoggedIn
        Log.d("MainActivity", "User logged in status: $isLoggedIn")

        if (isLoggedIn) {
            val username = sessionManager.username
            val userId = sessionManager.userId

            if (username.isNullOrEmpty() || userId.isNullOrEmpty()) {
                Log.w("MainActivity", "Session data incomplete, forcing logout")
                sessionManager.logoutUser()
                showLoginScreen()
            } else {
                // 🔹 Tambahan fitur: cek apakah akun masih ada di database
                checkIfAccountStillExists(username)
            }
        } else {
            Log.d("MainActivity", "User not logged in, showing login screen")
            showLoginScreen()
        }
    }

    /** 🔹 Mengecek apakah akun user masih ada di database */
    private fun checkIfAccountStillExists(username: String) {
        val url = "http://ortuconnect.atwebpages.com/api/profile.php?username=$username"
        Log.d("CheckAccount", "Checking account existence for $username")

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val success = response.optBoolean("success", false)
                if (success) {
                    Log.d("CheckAccount", "Account still exists, going to dashboard")
                    startDashboard()
                } else {
                    Log.w("CheckAccount", "Account not found, logging out")
                    Toast.makeText(this, "Akun sudah dihapus, silakan login ulang", Toast.LENGTH_LONG).show()
                    sessionManager.logoutUser()
                    showLoginScreen()
                }
            },
            { error ->
                Log.e("CheckAccount", "Error checking account: ${error.message}")
                // Jika koneksi gagal, tetap lanjutkan (agar tidak memblokir pengguna)
                startDashboard()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun showLoginScreen() {
        Log.d("MainActivity", "Showing login screen")
        setContent {
            LoginScreen(sessionManager)
        }
    }

    private fun startDashboard() {
        Log.d("MainActivity", "Starting dashboard")
        val intent = Intent(this, NavigasiCard::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
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
                            Log.d("Login", "Attempting login for username: ${username.trim()}")

                            val url = "http://ortuconnect.atwebpages.com/api/login.php"

                            try {
                                val params = JSONObject().apply {
                                    put("username", username.trim())
                                    put("password", password)
                                }

                                val request = object : JsonObjectRequest(
                                    Method.POST, url, params,
                                    Response.Listener { response ->
                                        isLoading = false

                                        Log.d("Login", "Server response: ${response.toString(2)}")

                                        val success = response.optBoolean("success", false)
                                        val message = response.optString("message", "Terjadi kesalahan server")

                                        if (success) {
                                            val user = response.optJSONObject("user")

                                            if (user != null) {
                                                val userId = user.optString("id_akun", "")
                                                val serverUsername = user.optString("username", username.trim())
                                                val role = user.optString("role", "")

                                                sessionManager.createLoginSession(serverUsername, userId, role)

                                                Toast.makeText(context, "Login berhasil!", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(context, NavigasiCard::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                context.startActivity(intent)
                                                (context as? ComponentActivity)?.finish()
                                            } else {
                                                Toast.makeText(context, "Format respons tidak valid", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    Response.ErrorListener { error ->
                                        isLoading = false
                                        Log.e("Login", "Volley Error: ${error.message}", error)
                                        Toast.makeText(context, "Gagal koneksi ke server", Toast.LENGTH_LONG).show()
                                    }
                                ) {}

                                Volley.newRequestQueue(context).add(request)
                            } catch (e: Exception) {
                                isLoading = false
                                Log.e("Login", "Exception: ${e.message}", e)
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
