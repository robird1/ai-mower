package com.ulsee.mower

import com.google.gson.Gson
import com.ulsee.mower.data.model.DesiredBool
import com.ulsee.mower.data.model.IotCoreShadowPayload
import org.junit.Assert.*
import org.junit.Test
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testDesiredRainMode() {
        val message = "{\"state\":{\"desired\":{\"rainmode\":\"ON\"}},\"metadata\":{\"desired\":{\"rainmode\":{\"timestamp\":1629275090}}},\"version\":60,\"timestamp\":1629275090}"
        val payload = Gson().fromJson(message, IotCoreShadowPayload::class.java)
        assertEquals(DesiredBool.yes, payload.state.desired?.rainmode)
    }

    @Test
    fun testCookie() {
        val cookieString = "robot=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2NvdW50Ijp7InVzZXJpZCI6NSwiY3JlYXRldGltZSI6IjIwMjEtMDgtMDNUMTQ6MjM6MjkrMDg6MDAiLCJhY2Nlc3N0aW1lIjoiMjAyMS0wOC0xM1QxMTowMToxMiswODowMCIsInN0YXR1cyI6Im5vcm1hbCIsInJvbGUiOiJ1c2VyIiwidXNlcm5hbWUiOiJjb2R1cy5oc3VAdWxzZWUuY29tIiwicGFzc3dvcmQiOiJlMTBhZGMzOTQ5YmE1OWFiYmU1NmUwNTdmMjBmODgzZSIsImdyb3VwbmFtZSI6InVsc2VlIiwiZGlzcGxheW5hbWUiOiJjb2R1cy5oc3VAdWxzZWUuY29tIiwiY29tcGFueSI6IiIsImVtYWlsIjoiY29kdXMuaHN1QHVsc2VlLmNvbSIsInBob25lIjoiIiwiY291bnRyeSI6IlRBSVdBTiJ9LCJleHAiOjE2Mjg4NDE4MjUsImlhdCI6MTYyODgyMzgyNX0.cUrBt43MlS1bf-gIsm320HZ9AGHbe6wey_KDQeEzNEo; Path=/; Expires=Fri, 13 Aug 2021 16:03:45 GMT; Secure; SameSite=None"
        val beginIdx = cookieString.indexOf("Expires=")+8
        val endIdx = cookieString.indexOf(";", beginIdx)
        // Expires=Fri, 13 Aug 2021 16:03:45 GMT;
        val expires = cookieString.substring(beginIdx, endIdx)
        assertEquals("Fri, 13 Aug 2021 16:03:45 GMT", expires)

        val df: java.text.DateFormat = java.text.SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
        df.setTimeZone(java.util.TimeZone.getTimeZone("GMT+08:00"))

        try {
            // Convert string into Date
            val expiredAt: java.util.Date = df.parse(expires)
            assertEquals(true, Date().before(expiredAt))
            println("expiredat = ")
            print(expiredAt)
        } catch (e: java.text.ParseException) {
            assertEquals(null, e.message)
            e.printStackTrace()
        }


    }
}