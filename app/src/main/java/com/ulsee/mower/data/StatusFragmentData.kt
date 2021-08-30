package com.ulsee.mower.data

class StatusFragmentBroadcast {
    companion object {
        const val LIFECYCLE_ONRESUME = "SFB_LIFECYCLE_ONRESUME" // resume顯示該畫面時，才回應alexa desired action
        const val LIFECYCLE_ONPAUSE = "SFB_LIFECYCLE_ONPAUSE"

        // 需要監控頁面的割草機狀態，做出不同判斷
        const val MOWER_STATUS_MOWING = "SFB_MOWER_STATUS_MOWING"
        const val MOWER_STATUS_PAUSE = "SFB_MOWER_STATUS_PAUSE"
        const val MOWER_STATUS_STOP = "SFB_MOWER_STATUS_STOP"
    }
}