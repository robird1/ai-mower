package com.ulsee.mower.ui.login

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ulsee.mower.R
import com.ulsee.mower.data.AccountRepository
import com.ulsee.mower.data.Result
import kotlinx.coroutines.launch

class LoginViewModel(private val loginRepository: AccountRepository) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    private val _requestResetPasswordResult = MutableLiveData<ActionResult>()
    val requestResetPasswordResult: LiveData<ActionResult> = _requestResetPasswordResult

    private val _resetPasswordResult = MutableLiveData<ActionResult>()
    val resetPasswordResult: LiveData<ActionResult> = _resetPasswordResult

    fun login(email: String, password: String = "") {
        viewModelScope.launch {
            val result = loginRepository.login(email, password)
            if (result is Result.Success) {
                _loginResult.value = LoginResult(success = LoggedInUserView(displayName = result.data.displayname))
            } else {
                _loginResult.value = LoginResult(error = (result as Result.Error).exception.message)
            }
        }
    }

    fun register(email: String, password: String = "") {
        viewModelScope.launch {
            val result = loginRepository.register(email, password)

            if (result is Result.Success) {
                _loginResult.value = LoginResult(success = LoggedInUserView(displayName = result.data.result))
            } else {
                _loginResult.value = LoginResult(error = (result as Result.Error).exception.message)
            }
        }
    }

    fun requestResetPassword(email: String) {
        viewModelScope.launch {
            val result = loginRepository.requestResetPassword(email)

            if (result is Result.Success) {
                _requestResetPasswordResult.value = ActionResult(null)
            } else {
                _requestResetPasswordResult.value = ActionResult(error = (result as Result.Error).exception.message)
            }
        }
    }

    fun resetPassword(email: String, secret: String, password: String) {
        viewModelScope.launch {
            val result = loginRepository.resetPassword(email, secret, password)

            if (result is Result.Success) {
                _resetPasswordResult.value = ActionResult(null)
            } else {
                _resetPasswordResult.value = ActionResult(error = (result as Result.Error).exception.message)
            }
        }
    }

    fun loginDataChanged(email: String, password: String = "") {
        if (!isEmailValid(email)) {
            _loginForm.value = LoginFormState(emailError = R.string.invalid_username)
        }
//        else if (!isPasswordValid(password)) {
//            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
//        }
        else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isEmailValid(email: String): Boolean {
        return if (email.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(email).matches()
        } else {
            email.isNotBlank()
        }
    }

    // A placeholder password validation check
//    private fun isPasswordValid(password: String): Boolean {
//        return password.length > 5
//    }
}