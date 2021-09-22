package com.ulsee.mower.ui.login

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.data.AccountDataSource
import com.ulsee.mower.data.AccountRepository

/**
 * ViewModel provider factory to instantiate LoginViewModel.
 * Required given LoginViewModel has a non-empty constructor
 */
class LoginViewModelFactory(val prefs: SharedPreferences) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                    loginRepository = AccountRepository(
                        dataSource = AccountDataSource("https://fr.ulsee.club/api/", prefs),
                        prefs = prefs
                    )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}