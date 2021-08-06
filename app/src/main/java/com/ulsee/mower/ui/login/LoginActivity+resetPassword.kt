package com.ulsee.mower.ui.login

import android.app.Dialog
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.ulsee.mower.R
import kotlinx.coroutines.async


fun LoginActivity.showForgetPasswordDialogStep1() {
    val adb: AlertDialog.Builder = AlertDialog.Builder(this).setCancelable(false)
    val d: Dialog = adb.setView(R.layout.dialog_resetpassword_step1).create()
    val lp = WindowManager.LayoutParams()
    lp.copyFrom(d.window!!.attributes)
    lp.width = resources.getDimensionPixelSize(R.dimen.dialog_width_resetpassword)//WindowManager.LayoutParams.MATCH_PARENT
    lp.height = WindowManager.LayoutParams.WRAP_CONTENT
    d.show()
    d.window!!.attributes = lp

    d.findViewById<Button>(R.id.button_cancel).setOnClickListener {
        d.dismiss()
    }

    d.findViewById<Button>(R.id.button_submit).setOnClickListener {
        val username = d.findViewById<EditText>(R.id.username)
        val loading = d.findViewById<ProgressBar>(R.id.loading)

        loading.visibility = View.VISIBLE
        val owner = this
        loginViewModel.requestResetPasswordResult.observe(owner, {
            loginViewModel.requestResetPasswordResult.removeObservers(owner)
            loading.visibility = View.GONE
            if (it.error == null) {
                showForgetPasswordDialogStep2(username.text.toString())
                d.dismiss()
            } else {
                Toast.makeText(this@showForgetPasswordDialogStep1, "failed to reset password: ${it.error}", Toast.LENGTH_SHORT).show()
            }
        })
        loginViewModel.requestResetPassword(username.text.toString())
    }
}

fun LoginActivity.showForgetPasswordDialogStep2(email: String) {
    val adb: AlertDialog.Builder = AlertDialog.Builder(this).setCancelable(false)
    val d: Dialog = adb.setView(R.layout.dialog_resetpassword_step2).create()
    val lp = WindowManager.LayoutParams()
    lp.copyFrom(d.window!!.attributes)
    lp.width = resources.getDimensionPixelSize(R.dimen.dialog_width_resetpassword)//WindowManager.LayoutParams.MATCH_PARENT
    lp.height = WindowManager.LayoutParams.WRAP_CONTENT
    d.show()
    d.window!!.attributes = lp

    val newPassword = d.findViewById<EditText>(R.id.newPassword)
    val confirmPassword = d.findViewById<EditText>(R.id.confirmPassword)
    val code = d.findViewById<EditText>(R.id.code)
    val loading = d.findViewById<ProgressBar>(R.id.loading)

    d.findViewById<Button>(R.id.button_cancel).setOnClickListener {
        d.dismiss()
    }

    d.findViewById<Button>(R.id.button_submit).setOnClickListener {
        if (newPassword.text.toString() != confirmPassword.text.toString()) {
            Toast.makeText(this@showForgetPasswordDialogStep2, "Passwords do not match!", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }

        loading.visibility = View.VISIBLE
        val owner = this
        loginViewModel.resetPasswordResult.observe(owner, {
            loginViewModel.resetPasswordResult.removeObservers(owner)
            loading.visibility = View.GONE
            if (it.error == null) {
                Toast.makeText(this@showForgetPasswordDialogStep2, "password has be reset", Toast.LENGTH_LONG).show()
                d.dismiss()
            } else {
                Toast.makeText(this@showForgetPasswordDialogStep2, "failed to reset password: ${it.error}", Toast.LENGTH_SHORT).show()
            }
        })
        loginViewModel.resetPassword(email, code.text.toString(), newPassword.text.toString())
    }
}
