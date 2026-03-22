package dev.elainedb.ytdash_android_codex

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dev.elainedb.ytdash_android_codex.utils.ConfigHelper

class LoginActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var errorTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        googleSignInClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        )

        errorTextView = findViewById(R.id.errorTextView)
        findViewById<Button>(R.id.signInButton).setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, REQUEST_SIGN_IN)
        }

        GoogleSignIn.getLastSignedInAccount(this)?.email?.let { email ->
            if (email in ConfigHelper.getAuthorizedEmails(this)) {
                navigateToMain()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_SIGN_IN || resultCode != Activity.RESULT_OK) return

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = runCatching { task.getResult(ApiException::class.java) }.getOrNull()
        val email = account?.email.orEmpty()

        if (email in ConfigHelper.getAuthorizedEmails(this)) {
            Log.i(TAG, "Access granted to $email")
            navigateToMain()
        } else {
            errorTextView.text = getString(R.string.login_error_unauthorized)
            googleSignInClient.signOut()
        }
    }

    private fun navigateToMain() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    companion object {
        private const val REQUEST_SIGN_IN = 1001
        private const val TAG = "LoginActivity"
    }
}
