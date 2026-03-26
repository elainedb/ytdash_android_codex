package dev.elainedb.ytdash_android_codex

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var errorText: TextView

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            handleSignInResult(account)
        } catch (_: ApiException) {
            showError(getString(R.string.sign_in_failed))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        googleSignInClient = ServiceLocator.googleSignInClient(this)
        errorText = findViewById(R.id.errorText)
        findViewById<Button>(R.id.signInButton).setOnClickListener {
            errorText.text = ""
            signInLauncher.launch(googleSignInClient.signInIntent)
        }

        GoogleSignIn.getLastSignedInAccount(this)?.let(::handleSignInResult)
    }

    private fun handleSignInResult(account: GoogleSignInAccount?) {
        val email = account?.email
        if (email.isNullOrBlank()) {
            showError(getString(R.string.missing_email))
            return
        }

        val authorizedEmails = ServiceLocator.configHelper().getAuthorizedEmails()
        if (email in authorizedEmails) {
            Log.i("LoginActivity", "Access granted to $email")
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }

        showError(getString(R.string.access_denied))
        googleSignInClient.signOut()
    }

    private fun showError(message: String) {
        errorText.text = message
    }
}
