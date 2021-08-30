package com.ulsee.mower.data

class BLECommandTable {
    companion object {
        const val VERIFICATION = 16
        const val MOVE = 0x20
        const val SCHEDULE = -16//0xF0//0x30
        const val START_STOP = 0X40
        const val STATUS = 80
        const val MAP_DATA = 0X60
        const val RECORD_BOUNDARY = 0X70
        const val SETTINGS = -128       // 0x80.toByte() -> -128
        const val DELETE_MAP = -112     // 0x90.toByte() -> -112
        const val MOWING_DATA = -80     // 0xB0.toByte() -> -80
    }
}

class Status {
    class WorkingMode {
        companion object {
            const val MANUAL_MODE = 0x00
            const val WORKING_MODE = 0x01
            const val LEARNING_MODE = 0x02
            const val TESTING_BOUNDARY_MODE = 0X03
            const val SUSPEND_WORKING_MODE = 0x04
        }
    }

    class TestingBoundaryState {
        companion object {
            const val NONE = 0x00
            const val WAITING = 0x01          // 等待指令(饶边或保存)
            const val TESTING = 0x02          // 绕边
            const val TEST_FAILED = 0X03      // 绕边失败
            const val TEST_SUCCESS = 0x04     // 绕边成功
            const val TEST_CANCELLED = 0x05   // 取消绕边
        }
    }

    class WorkingErrorCode {
        companion object {
            val map = hashMapOf<Int, String>()
            init {
                map[0] = "success"
                map[1] = "充电座内惯导初始化超时"
                map[2] = "草坪内惯导初始化超时"
                map[3] = "路径上有障碍物"
                map[4] = "沿路径行走超时"
                map[5] = "找不到回充路径"
                map[6] = "前往返回路径的起点时超时"
                map[7] = "进入充电座失败"
                map[8] = "充电接触失败"
                map[9] = "启动失败，缺少回充路径"
                map[10] = "启动失败，不在充电桩或草坪内"
                map[11] = "启动失败，没有需要作业的草坪"
                map[12] = "初始化失败，请手动控制回充电桩或重新启动"
                map[13] = "学习失败，正在尝试返回充电桩"
                map[14] = "请确认割草機位置远离边界及障碍物1.2m以上，再啟動割草機"
                map[15] = "启动失败，请放置于充电桩或围线边缘/内部"
                map[16] = "电量过低，不建议启动，请充电"
                map[255] = "unknown error"
            }
        }
    }

    class Interruption {
        companion object {
            val map = hashMapOf<Int, String>()
            init {
                map[0] = "受困"
                map[1] = "抬升"
                map[2] = "出界"
                map[3] = "围线信号丢失"
                map[4] = "翻转"
                map[5] = "按键急停"
                map[6] = ""
                map[7] = ""
                map[8] = "电机驱动异常"
                map[9] = "下雨"
                map[10] = "割草电机温度过高"
                map[11] = "围线检测传感器故障"
                map[12] = "刹车异常"
                map[13] = "割草电机堵转"
                map[14] = "电池温度过高"
                map[15] = "亏电返回充电桩"
                map[16] = "温度过低暂停任务"
                map[17] = "温度过高暂停任务"
                map[18] = "温度过低无法充电"
                map[19] = "温度过高无法充电"
                map[20] = "围线接反"
                map[21] = "右轮电机异常"
                map[22] = "左轮电机异常"
                map[23] = "割草电机电流异常"
            }
        }
    }

    class RobotStatus {
        companion object {
            val map = hashMapOf<Int, String>()
            init {
                map[0] = "左轮电机故障"
                map[1] = "割草电机故障"
                map[2] = "受困"
                map[3] = "雨水"
                map[4] = "急停"
                map[5] = "右轮悬空"
                map[6] = "左轮悬空"
                map[7] = "翻转"
                map[8] = "右碰撞"
                map[9] = "左碰撞"
                map[10] = "路径规划失败"
                map[11] = "充电桩移位"
                map[12] = "出界"
                map[13] = "路径上有障碍物"
                map[14] = "电池过温"
                map[15] = "右轮电机故障"
                map[16] = "出界停机"
                map[17] = "围线信号丢失"
                map[18] = "勿扰模式标识"
                map[19] = "主基准源位置调整中"
                map[20] = "组合导航状态"
                map[21] = "刀盘开启"
                map[22] = "充电状态"
                map[23] = "定位状态"

                map[24] = "等待重新确认工作区域"
            }
        }
    }
}

class RecordBoundary {
    class Command {
        companion object {
            const val START_RECORD = 0x00
            const val FINISH_RECORD = 0x01
            const val START_POINT_MODE = 0x02
            const val SET_POINT = 0x03
            const val FINISH_POINT_MODE = 0x04
            const val CANCEL_RECORD = 0x05
            const val SAVE_BOUNDARY = 0x06
            const val DISCARD_BOUNDARY = 0x07
        }
    }
    class Subject {
        companion object {
            const val GRASS = 0x00
            const val OBSTACLE = 0x01
            const val CHARGING = 0x02
            const val GRASS_PATH = 0x03
            const val MOWING_DIRECTION = 0x04
        }
    }
    class ErrorCode {
        companion object {
            val map = hashMapOf<Int, String>()
            init {
                map[0] = "failed"
                map[1] = "success"
                map[2] = "lawn boundary does not satisfy requirement"
                map[3] = "FLASH memory space is not enough"
                map[4] = "FLASH erase error"
                map[5] = "start point of obstacle is not within lawn boundary"
                map[6] = "obstacle boundary does not satisfy requirement"
                map[7] = "RTK is abnormal"
                map[8] = "start point of charging path is not within lawn boundary"
                map[9] = "repeatedly add charging path"
                map[10] = "start point of path between lawn areas is not within lawn boundary"
                map[11] = "end point of path between lawn areas is not within lawn boundary"
                map[12] = "start point and end point are in same lawn area"
                map[13] = "repeatedly add path between lawn areas"
                map[14] = "没有开始记录时点击结束记录"
                map[15] = "start point of mowing is not within lawn boundary"
                map[255] = "unknown error"
            }
        }
    }
}

class StartStop {
    class Command {
        companion object {
            const val START_MOWING = 0x00
            const val FORCE_LEARNING = 0x01
            const val BACK_CHARGING_STATION = 0x02
            const val RESUME_EMERGENCY_STOP = 0x03
            const val OBSTACLE_CLEARED = 0x04
            const val TEST_WORKING_BOUNDARY = 0x05
            const val CANCEL_TEST_WORKING_BOUNDARY = 0x06
            const val RESUME_FROM_STUCK = 0x07
            const val PAUSE_MOWING = 0x08
            const val RESUME_MOWING = 0x09
            const val RESUME_FROM_INTERRUPT = 0x0A
            const val RECONFIRM_WORKING_AREA = 0x0B
        }
    }
    class ErrorCode {
        companion object {
            val map = hashMapOf<Int, String>()
            init {
                map[0] = "表示接收异常"
                map[1] = "表示接受正常"
                map[2] = "too far away from boundary when testing working boundary"
                map[15] = "Start failed. Please place mower at charging station"
                map[16] = "Low energy. Please charge the mower first"
                map[17] = "请确认围线连接正常"
                map[18] = "Start failed. Rain is detected"
                map[19] = "回充失败，请将小车移动到围线内部"
                map[20] = "亏电中,等待充电完毕将会自动继续"
                map[31] = "表示当前处于勿扰模式"
            }
        }
    }
}

class BLEBroadcastAction {
    companion object {
        const val ACTION_CONNECT_FAILED = "action_connect_failed"
        const val ACTION_DEVICE_NOT_FOUND = "action_device_not_found"
        const val ACTION_GATT_CONNECTED = "action_gatt_connected"
        const val ACTION_GATT_DISCONNECTED = "action_gatt_disconnected"
        const val ACTION_GATT_NOT_SUCCESS = "action_gatt_not_success"
        const val ACTION_ON_DISCONNECT_DEVICE = "action_on_disconnect_device"
        const val ACTION_VERIFICATION_SUCCESS = "action_verification_success"
        const val ACTION_VERIFICATION_FAILED = "action_verification_failed"
        const val ACTION_STATUS = "action_status"
        const val ACTION_BORDER_RECORD = "action_border_record"
        const val ACTION_START_STOP = "action_start_stop"
        const val ACTION_GLOBAL_PARAMETER = "action_global_parameter"
        const val ACTION_GRASS_BOARDER = "action_grass_boarder"
        const val ACTION_OBSTACLE_BOARDER = "action_obstacle_boarder"
        const val ACTION_GRASS_PATH = "action_grass_path"
        const val ACTION_CHARGING_PATH = "action_charging_path"
        const val ACTION_REQUEST_DELETE_MAP = "action_request_delete_map"
        const val ACTION_RESPONSE_DELETE_MAP = "action_response_delete_map"
        const val ACTION_MOWING_DATA = "action_mowing_data"
        const val ACTION_SETTINGS = "action_settings"
        const val ACTION_SCHEDULING = "action_scheduling"
    }
}