package dev.elainedb.ytdash_android_codex

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import dev.elainedb.ytdash_android_codex.core.error.Failure
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.repository.AuthRepository
import dev.elainedb.ytdash_android_codex.domain.usecase.SignInWithGoogle
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var signInWithGoogle: SignInWithGoogle

    private lateinit var signInButton: Button
    private lateinit var errorText: TextView

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        lifecycleScope.launch {
            val outcome = try {
                signInWithGoogle(task.getResult(ApiException::class.java))
            } catch (error: Exception) {
                Result.Error(Failure.Auth(error.message ?: getString(R.string.error_sign_in_failed)))
            }

            when (outcome) {
                is Result.Success -> navigateToMain()
                is Result.Error -> errorText.text = outcome.failure.message
            }
            signInButton.isEnabled = true
            signInButton.text = getString(R.string.sign_in_with_google)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        signInButton = findViewById(R.id.signInButton)
        errorText = findViewById(R.id.errorText)

        authRepository.getSignedInUser()?.let {
            navigateToMain()
            return
        }

        signInButton.setOnClickListener {
            errorText.text = ""
            signInButton.isEnabled = false
            signInButton.text = getString(R.string.signing_in)
            signInLauncher.launch(authRepository.getSignInIntent())
        }
    }

    private fun navigateToMain() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}
