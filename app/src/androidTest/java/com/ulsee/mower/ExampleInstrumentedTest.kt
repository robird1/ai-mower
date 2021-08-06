package com.ulsee.mower

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ulsee.mower.data.AccountDataSource
import com.ulsee.mower.data.AccountRepository
import com.ulsee.mower.data.Result
import com.ulsee.mower.data.model.isOK
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ulsee.testble", appContext.packageName)
    }

    @Test
    fun testLogin() {
        val prefs = InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("account", Context.MODE_PRIVATE)
        runBlocking {
            val loginRepository = AccountRepository(AccountDataSource("https://fr.ulsee.club/api/"), prefs)
            val result = loginRepository.login("codus.hsu@ulsee.com", "123456")

            assertEquals(true, (result is Result.Success))
            if (result is Result.Success) {
                assertEquals("codus.hsu@ulsee.com", result.data.email)
                assertEquals("robot=", result.data.cookie.substring(0,6))
            }
        }
    }
    @Test
    fun testRegister() {
        runBlocking {
            val prefs = InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("account", Context.MODE_PRIVATE)
            val loginRepository = AccountRepository(AccountDataSource("https://fr.ulsee.club/api/"), prefs)
            val result = loginRepository.register("codus.hsu.2@ulsee.com", "123456")

            assertEquals("", (result as Result.Error).exception.message)
            assertEquals(true, (result is Result.Success))
            if (result is Result.Success) {
                assertEquals("success", result.data.result)
                assertEquals(true, result.data.isOK)
            }
        }
    }
    @Test
    fun testLoginThenGetMe() {
        runBlocking {
            val prefs = InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("account", Context.MODE_PRIVATE)
            val loginRepository = AccountRepository(AccountDataSource("https://fr.ulsee.club/api/"), prefs)
            val result = loginRepository.login("codus.hsu@ulsee.com", "123456")

            assertEquals(true, (result is Result.Success))
            if (result is Result.Success) {
                assertEquals("codus.hsu@ulsee.com", result.data.email)
                assertEquals("robot=", result.data.cookie.substring(0,6))
            }

            val meResult = loginRepository.getMe()
//            assertEquals("", (meResult as Result.Error).exception.message)
            if (meResult is Result.Success) {
                assertEquals("codus.hsu@ulsee.com", meResult.data.email)
                assertEquals(true, (meResult as Result.Success).data.isOK)
            }
        }
    }

    @Test
    fun testLoginThenBind() {
        runBlocking {
            val prefs = InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("account", Context.MODE_PRIVATE)
            val loginRepository = AccountRepository(AccountDataSource("https://fr.ulsee.club/api/"), prefs)
            val result = loginRepository.login("codus.hsu@ulsee.com", "123456")

            assertEquals(true, (result is Result.Success))
            if (result is Result.Success) {
                assertEquals("codus.hsu@ulsee.com", result.data.email)
                assertEquals("robot=", result.data.cookie.substring(0,6))
            }

            val bindResult = loginRepository.bind("TNM78-FJKXR-P26YV-GP8MB-JK8XG")
            if (bindResult is Result.Success) {
                assertEquals(true, bindResult.data.isOK)
            } else {
                assertEquals("", (bindResult as Result.Error).exception.message)
            }
        }
    }

    @Test
    fun testLoginThenUnbind() {
        runBlocking {
            val prefs = InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("account", Context.MODE_PRIVATE)
            val loginRepository = AccountRepository(AccountDataSource("https://fr.ulsee.club/api/"), prefs)
            val result = loginRepository.login("codus.hsu@ulsee.com", "123456")

            assertEquals(true, (result is Result.Success))
            if (result is Result.Success) {
                assertEquals("codus.hsu@ulsee.com", result.data.email)
                assertEquals("robot=", result.data.cookie.substring(0,6))
            }

            val bindResult = loginRepository.unbind("TNM78-FJKXR-P26YV-GP8MB-JK8XG")
            if (bindResult is Result.Success) {
                assertEquals(true, bindResult.data.isOK)
            } else {
                assertEquals("", (bindResult as Result.Error).exception.message)
            }
        }
    }
}