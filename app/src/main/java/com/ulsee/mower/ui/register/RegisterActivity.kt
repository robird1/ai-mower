package com.ulsee.mower.ui.register

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ulsee.mower.R
import com.ulsee.mower.ui.login.*
import kotlinx.coroutines.async

class RegisterActivity  : AppCompatActivity() {

    companion object {
        val TAG = "RegisterActivity"
    }
    private lateinit var registerViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        val username = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)
        val signUp = findViewById<Button>(R.id.signUp)
        val signIn = findViewById<View>(R.id.textView_subtitle_signin)
        val loading = findViewById<ProgressBar>(R.id.loading)
        val agreementCheckbox = findViewById<CheckBox>(R.id.checkbox_agreement)

        val prefs = getSharedPreferences("account", Context.MODE_PRIVATE)
        registerViewModel = ViewModelProvider(this, LoginViewModelFactory(prefs))
            .get(LoginViewModel::class.java)

        signIn.setOnClickListener{
            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
            finish()
        }

        signUp.setOnClickListener {

            if (!agreementCheckbox.isChecked) {
                Toast.makeText(
                    this@RegisterActivity,
                    R.string.hint_register_agreement_check,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (password.text.toString().length < 6) {
                password.error = getString(R.string.invalid_password)
                return@setOnClickListener
            }
            if (!password.text.toString().matches(Regex(".*\\d.*"))) {
                password.error = getString(R.string.invalid_password_no_number)
                return@setOnClickListener
            }

            register()
        }

        password.apply {
            afterTextChanged {
                registerViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        register()
                }
                false
            }
        }
        registerViewModel.loginResult.observe(this, {
            loading.visibility = View.GONE
            if (it.error != null) {
                showRegisterFailed(it.error)
            } else {

                val adb: AlertDialog.Builder = AlertDialog.Builder(this@RegisterActivity)
                val d: Dialog = adb.
                setTitle("Registration Success").
                setMessage(R.string.hint_login_after_register).
                setCancelable(false).
                setPositiveButton("ok") { dialog, which ->
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                }.
                create()
                d.show()
            }
        })
    }

    private fun showRegisterFailed(@StringRes errorString: String) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
    fun register() {
        val loading = findViewById<ProgressBar>(R.id.loading)
        val username = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)
        loading.visibility = View.VISIBLE

        registerViewModel.register(username.text.toString(), password.text.toString())
    }
}