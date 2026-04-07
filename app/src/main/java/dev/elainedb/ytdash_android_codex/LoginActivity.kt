package dev.elainedb.ytdash_android_codex

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.data.config.ConfigHelper
import dev.elainedb.ytdash_android_codex.domain.repository.AuthRepository
import dev.elainedb.ytdash_android_codex.domain.usecase.SignInWithGoogle
import dev.elainedb.ytdash_android_codex.domain.usecase.SignOut
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var configHelper: ConfigHelper
    @Inject lateinit var signInWithGoogle: SignInWithGoogle
    @Inject lateinit var signOut: SignOut

    private lateinit var errorText: TextView

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        lifecycleScope.launch {
            when (val signInResult = signInWithGoogle(result.data)) {
                is Result.Success -> {
                    val email = signInResult.data.email?.lowercase().orEmpty()
                    val authorizedEmails = configHelper.getConfig().authorizedEmails
                    if (authorizedEmails.contains(email)) {
                        Log.d("LoginActivity", "Access granted to $email")
                        startActivity(
                            Intent(this@LoginActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                    } else {
                        errorText.text = getString(R.string.access_denied)
                        signOut(Unit)
                    }
                }
                is Result.Error -> {
                    errorText.text = signInResult.failure.message
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        errorText = findViewById(R.id.errorTextView)
        findViewById<Button>(R.id.signInButton).setOnClickListener {
            errorText.text = ""
            signInLauncher.launch(authRepository.signInIntent())
        }
    }
}
