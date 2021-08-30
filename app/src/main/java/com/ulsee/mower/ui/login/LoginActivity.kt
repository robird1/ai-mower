package com.ulsee.mower.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.MainActivity
import com.ulsee.mower.R
import com.ulsee.mower.ui.register.RegisterActivity


class LoginActivity : AppCompatActivity() {

    lateinit var loginViewModel: LoginViewModel
    val registerActivityRequestCode = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val username = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.login)
        val loading = findViewById<ProgressBar>(R.id.loading)
//        username.setText("codus.hsu@ulsee.com")
//        password.setText("123456")

        loginButton.isEnabled = true

        val prefs = getSharedPreferences("account", Context.MODE_PRIVATE)
        loginViewModel = ViewModelProvider(this, LoginViewModelFactory(prefs))
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            if (loginState.emailError != null) {
                username.error = getString(loginState.emailError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        login()
                }
                false
            }
        }

        loginButton.setOnClickListener {
            login()
        }

        loginViewModel.loginResult.observe(this, {
            loading.visibility = View.GONE
            if (it.error != null) {
                showLoginFailed(it.error)
            } else {
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        })

        val signUp = findViewById<View>(R.id.textView_subtitle_signup)
        val forgetPassword = findViewById<View>(R.id.textView_forgetPassword)
        signUp.setOnClickListener{
            startActivityForResult(Intent(this@LoginActivity, RegisterActivity::class.java), registerActivityRequestCode)
            finish()
        }
        forgetPassword.setOnClickListener {
            showForgetPasswordDialogStep1()
        }
    }

    fun login() {
        val loading = findViewById<ProgressBar>(R.id.loading)
        val username = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)
        loading.visibility = View.VISIBLE

        loginViewModel.login(username.text.toString(), password.text.toString())
    }

    private fun showLoginFailed(@StringRes errorString: String) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == registerActivityRequestCode && resultCode == RESULT_OK) {
            finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}