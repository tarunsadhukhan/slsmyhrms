package com.example.slsHrms

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.slsHrms.api.ApiConfig
import com.example.slsHrms.api.ApiRoutes
import com.example.slsHrms.api.LoginRequest
import com.example.slsHrms.api.LoginResponse
import com.example.slsHrms.api.RetrofitClient
import com.example.slsHrms.databinding.ActivityLoginBinding
import com.example.slsHrms.permissions.PermissionManager
import com.example.slsHrms.sync.Connectivity
import com.example.slsHrms.sync.OfflineCredentials
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val PREFS_NAME = "LoginPrefs"
    private val KEY_LAST_USERNAME = "last_username"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show the build version (auto-incremented per APK build).
        binding.tvAppVersion.text = try {
            "v" + packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) { "" }

        setupToolbar()
        setupLoginButton()
        setupSettingsButton()
        loadPreviousUsername()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadPreviousUsername() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val lastUsername = sharedPreferences.getString(KEY_LAST_USERNAME, "")
        if (!lastUsername.isNullOrEmpty()) {
            binding.etUsername.setText(lastUsername)
            binding.etPassword.requestFocus()
        }
    }

    private fun setupSettingsButton() {
        binding.btnSettings.setOnClickListener {
            showApiConfigDialog()
        }
    }

    private fun showApiConfigDialog() {
        val currentUrl = ApiConfig.getConfiguredUrl(this)

        val input = EditText(this).apply {
            setText(currentUrl)
            hint = "e.g. http://127.0.0.1:5052/sls  (last part = database name)"
            setPadding(60, 40, 60, 40)
            setTextColor(android.graphics.Color.BLACK)
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Configure API URL")
            .setMessage("Enter server URL ending with the database name, e.g. http://127.0.0.1:5052/sls")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    ApiConfig.saveBaseUrl(this, url)
                    Toast.makeText(this, "API URL saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInputs(username, password)) {
                performLogin(username, password)
            }
        }
    }

    private fun validateInputs(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            binding.etUsername.error = "Please enter username"
            binding.etUsername.requestFocus()
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Please enter password"
            binding.etPassword.requestFocus()
            return false
        }
        return true
    }

    private fun performLogin(username: String, password: String) {
        // Known-offline: go straight to the cached credential instead of making
        // the operator wait out a connect timeout at a gate with no signal.
        if (!Connectivity.isOnline(this) && OfflineCredentials.isAvailable(this)) {
            tryOfflineLogin(username, password, "no network")
            return
        }
        sendLogin(username, password)
    }

    private fun sendLogin(username: String, password: String) {
        showLoading(true)

        val loginRequest = LoginRequest(username, password)

        RetrofitClient.getApiService(this).login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                showLoading(false)

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null && loginResponse.isSuccess) {
                        // Save username and user_id to SharedPreferences
                        saveUsername(username)
                        val userId = loginResponse.user?.id
                        userId?.let {
                            getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                                .edit().putInt("user_id", it).apply()
                        }

                        // Remember this (server-verified) sign-in so the same
                        // person can get in with no network later — a phone that
                        // reboots at a gate must still reach the attendance
                        // screen. Only a PBKDF2 hash is stored, never the password.
                        OfflineCredentials.remember(
                            this@LoginActivity, username, password, userId,
                            loginResponse.user?.fullName, ApiConfig.getTenant(this@LoginActivity)
                        )

                        Toast.makeText(
                            this@LoginActivity,
                            "Login successful! Welcome ${loginResponse.user?.fullName ?: username}",
                            Toast.LENGTH_SHORT
                        ).show()

                        val openDashboard = {
                            val dashboardIntent = Intent(this@LoginActivity, DashboardActivity::class.java)
                            dashboardIntent.putExtra("USER_NAME", loginResponse.user?.fullName ?: username)
                            startActivity(dashboardIntent)
                            finish()
                        }

                        if (userId != null) {
                            // Pull menu permissions before opening the dashboard
                            // so menus the user can't see never flash on screen.
                            // On refresh failure we fall back to the cached value
                            // (PermissionManager.loadFromCacheIfNeeded handles that).
                            PermissionManager.refresh(this@LoginActivity, userId) { _ ->
                                openDashboard()
                            }
                        } else {
                            openDashboard()
                        }
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            loginResponse?.message ?: "Login failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                showLoading(false)
                // Server unreachable — not "wrong password". Fall back to the
                // credentials this device has already had verified.
                tryOfflineLogin(username, password, t.localizedMessage)
            }
        })
    }

    /**
     * Sign in against the credential cached from the last successful online
     * login. This is the only way an operator can reach the attendance screen
     * after a reboot at a gate with no signal.
     */
    private fun tryOfflineLogin(username: String, password: String, networkError: String?) {
        val tenant = ApiConfig.getTenant(this)
        when (val result = OfflineCredentials.verify(this, username, password, tenant)) {
            is OfflineCredentials.Result.Ok -> {
                val session = result.session
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putInt("user_id", session.userId)
                    .apply()
                saveUsername(session.username)
                Toast.makeText(
                    this,
                    "Signed in offline as ${session.fullName}. " +
                        "Attendance and onboarding will save on this device.",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(this, DashboardActivity::class.java)
                intent.putExtra("USER_NAME", session.fullName)
                startActivity(intent)
                finish()
            }
            is OfflineCredentials.Result.WrongPassword ->
                Toast.makeText(this, "Wrong password (checked offline)", Toast.LENGTH_LONG).show()
            is OfflineCredentials.Result.Expired ->
                Toast.makeText(
                    this,
                    "Offline sign-in expired — last online login was ${result.days} days ago. " +
                        "Connect to the server once to continue.",
                    Toast.LENGTH_LONG
                ).show()
            is OfflineCredentials.Result.WrongTenant ->
                Toast.makeText(
                    this,
                    "This device is configured for a different database. " +
                        "Connect to the server to sign in.",
                    Toast.LENGTH_LONG
                ).show()
            is OfflineCredentials.Result.NotEnrolled ->
                Toast.makeText(
                    this,
                    "No network, and this user has not signed in on this device before.\n" +
                        "(${networkError ?: "server unreachable"})",
                    Toast.LENGTH_LONG
                ).show()
        }
    }

    private fun saveUsername(username: String) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sharedPreferences.edit().putString(KEY_LAST_USERNAME, username).apply()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLogin.alpha = if (isLoading) 0.6f else 1.0f
    }
}
