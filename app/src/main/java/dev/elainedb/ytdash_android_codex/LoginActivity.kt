package dev.elainedb.ytdash_android_codex

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.AndroidEntryPoint
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.usecase.SignInParams
import dev.elainedb.ytdash_android_codex.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    @Inject lateinit var signInWithGoogleUseCase: SignInWithGoogleUseCase

    private val googleSignInClient by lazy {
        GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        )
    }

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            lifecycleScope.launch {
                val taskResult = runCatching { account.result }
                if (taskResult.isFailure) {
                    showError(getString(R.string.login_error_generic))
                    return@launch
                }

                when (
                    val signInResult = signInWithGoogleUseCase(
                        SignInParams(
                            email = taskResult.getOrNull()?.email,
                            displayName = taskResult.getOrNull()?.displayName
                        )
                    )
                ) {
                    is Result.Success -> navigateToMain()
                    is Result.Error -> showError(signInResult.failure.message)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<Button>(R.id.googleSignInButton).setOnClickListener {
            showError("")
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun navigateToMain() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    private fun showError(message: String) {
        findViewById<TextView>(R.id.errorText).text = message
    }
}
