package com.example.mobiles_tktech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.viewinterop.AndroidView
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
import com.example.mobiles_tktech.navigasi.NavigasiCard

class SessionManager(context: Context) {
    companion object {
        private const val PREF_NAME = "LoginPrefs"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USERNAME = "username"
        private const val KEY_ID_SISWA = "id_siswa"
        private const val KEY_ROLE = "role"
    }

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun createLoginSession(username: String, idSiswa: Int = 0, role: String = "") {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USERNAME, username)
        editor.putInt(KEY_ID_SISWA, idSiswa)
        editor.putString(KEY_ROLE, role)
        editor.apply()
    }

    fun getUsername(): String = sharedPreferences.getString(KEY_USERNAME, "") ?: ""
    fun getIdSiswa(): Int = sharedPreferences.getInt(KEY_ID_SISWA, 0)
    fun getRole(): String = sharedPreferences.getString(KEY_ROLE, "") ?: ""
    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    fun logoutUser() {
        editor.clear()
        editor.apply()
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(applicationContext)

        if (sessionManager.isLoggedIn()) {
            startDashboard()
            return
        }

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

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            factory = { ctx ->
                android.widget.ImageView(ctx).apply {
                    setImageResource(R.drawable.background_gradient_blue_purple)
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                }
            },
            modifier = Modifier.fillMaxSize()
        )

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

                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.logo_ortuconnect),
                    contentDescription = "Logo",
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
                            val params = JSONObject().apply {
                                put("username", username)
                                put("password", password)
                            }

                            val request = JsonObjectRequest(
                                Request.Method.POST, url, params,
                                { response ->
                                    isLoading = false
                                    val success = response.optBoolean("success")
                                    val message = response.optString("message")
                                    if (success) {
                                        val user = response.optJSONObject("user")
                                        val idSiswa = user?.optInt("id_siswa", 0) ?: 0
                                        val usernameResp = user?.optString("username", username) ?: username
                                        val role = user?.optString("role", "ortu") ?: "ortu"

                                        sessionManager.createLoginSession(usernameResp, idSiswa, role)
                                        Toast.makeText(context, "Login berhasil", Toast.LENGTH_SHORT).show()

                                        context.startActivity(Intent(context, NavigasiCard::class.java))
                                        (context as? ComponentActivity)?.finish()
                                    } else {
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                { error ->
                                    isLoading = false
                                    Toast.makeText(context, "Gagal koneksi ke server", Toast.LENGTH_LONG).show()
                                }
                            )

                            Volley.newRequestQueue(context).add(request)
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
