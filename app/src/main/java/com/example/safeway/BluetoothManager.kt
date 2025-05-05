package com.example.safeway

import android.bluetooth.BluetoothSocket

object BluetoothManager {
    var socket: BluetoothSocket? = null
    val isConnected: Boolean
        get() = socket?.isConnected == true

    fun sendMessage(message: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                val out = socket?.outputStream
                val `in` = socket?.inputStream

                if (out != null && `in` != null) {
                    out.write(message.toByteArray())
                    val buffer = ByteArray(1024)
                    val bytes = `in`.read(buffer)
                    val response = String(buffer, 0, bytes)
                    onResult(response)
                } else {
                    onError("출력 스트림이 유효하지 않습니다.")
                }
            } catch (e: Exception) {
                onError("전송 실패: ${e.message}")
            }
        }.start()
    }
}
