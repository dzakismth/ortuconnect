package com.example.mobiles_tktech

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.mobiles_tktech.dashboard.DashboardActivity
import com.example.mobiles_tktech.ui.theme.MobilesTktechTheme
import org.json.JSONObject

// Session Manager untuk menyimpan status login & data user
class SessionManager(context: Context) {
    companion object {
        private const val PREF_NAME = "LoginPrefs"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USERNAME = "username"
    }

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun createLoginSession(isLoggedIn: Boolean, username: String) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }

    fun getUsername(): String {
        return sharedPreferences.getString(KEY_USERNAME, "") ?: ""
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logoutUser() {
        editor.clear()
        editor.apply()
    }
}

class MainActivitycok : ComponentActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(applicationContext)

        // Jika sudah login, langsung ke dashboard
        if (sessionManager.isLoggedIn()) {
            startDashboardActivity()
            return
        }

        setContent {
            MobilesTktechTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(sessionManager)
                }
            }
        }
    }

    private fun startDashboardActivity() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun LoginScreen(sessionManager: SessionManager) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF007ABF),
                        Color(0xFF45287F),
                        Color(0xFF3E2371),
                        Color(0xFF68327E)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_ortuconnect),
                    contentDescription = "Logo",
                    modifier = Modifier.size(200.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Masukkan Username") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Masukkan Kata Sandi") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )

                Button(
                    onClick = {
                        if (username.isEmpty() || password.isEmpty()) {
                            Toast.makeText(context, "Isi semua kolom", Toast.LENGTH_SHORT).show()
                        } else {
                            isLoading = true
                            val url = "http://ortuconnect.atwebpages.com/api/login.php"

                            val params = JSONObject()
                            params.put("username", username)
                            params.put("password", password)

                            val request = JsonObjectRequest(
                                Request.Method.POST, url, params,
                                { response ->
                                    isLoading = false
                                    val success = response.optBoolean("success")
                                    val message = response.optString("message")

                                    if (success) {
                                        // Simpan session login & username
                                        sessionManager.createLoginSession(true, username)

                                        Toast.makeText(context, "Login Berhasil", Toast.LENGTH_SHORT).show()

                                        val activity = context as? ComponentActivity
                                        val intent = Intent(context, DashboardActivity::class.java)
                                        context.startActivity(intent)
                                        activity?.finish()
                                    } else {
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                { error ->
                                    isLoading = false
                                    Toast.makeText(context, "Terjadi kesalahan koneksi", Toast.LENGTH_SHORT).show()
                                }
                            )

                            val queue = Volley.newRequestQueue(context)
                            queue.add(request)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Masuk")
                    }
                }
            }
        }
    }
}
