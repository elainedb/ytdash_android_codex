package dev.elainedb.ytdash_android_codex

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dev.elainedb.ytdash_android_codex.config.ConfigHelper

class LoginActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var errorText: TextView
    private lateinit var signInButton: Button

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        val account = runCatching { task.getResult(ApiException::class.java) }.getOrElse {
            errorText.setText(R.string.sign_in_failed)
            googleSignInClient.signOut()
            signInButton.isEnabled = true
            signInButton.setText(R.string.sign_in_with_google)
            return@registerForActivityResult
        }
        val email = account?.email
        val config = ConfigHelper.loadConfig(this)

        when {
            email.isNullOrBlank() -> {
                errorText.setText(R.string.missing_email)
                googleSignInClient.signOut()
            }

            email in config.authorizedEmails -> {
                Log.i("LoginActivity", "Access granted to $email")
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            }

            else -> {
                errorText.setText(R.string.access_denied)
                googleSignInClient.signOut()
            }
        }
        signInButton.isEnabled = true
        if (signInButton.text != getString(R.string.sign_in_with_google)) {
            signInButton.setText(R.string.sign_in_with_google)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        errorText = findViewById(R.id.errorText)
        signInButton = findViewById(R.id.signInButton)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        GoogleSignIn.getLastSignedInAccount(this)?.email?.let { email ->
            val config = ConfigHelper.loadConfig(this)
            if (email in config.authorizedEmails) {
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                return
            }
            googleSignInClient.signOut()
        }

        signInButton.setOnClickListener {
            errorText.text = ""
            signInButton.isEnabled = false
            signInButton.setText(R.string.signing_in)
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }
}
