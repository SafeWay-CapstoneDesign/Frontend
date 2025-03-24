package com.example.safeway

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class TestBluetoothMessageActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val serverDeviceName = "raspberrypi"
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_bluetooth_message)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissions()
        } else {
            initBluetoothConnection()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )

        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.all { it.value }) {
                initBluetoothConnection()
            } else {
                Toast.makeText(this, "블루투스 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        }

        permissionLauncher.launch(permissions)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun initBluetoothConnection() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            bluetoothAdapter.enable()
            Toast.makeText(this, "블루투스를 활성화합니다.", Toast.LENGTH_SHORT).show()
        }

        Toast.makeText(this, "페어링된 장치를 검색 중...", Toast.LENGTH_SHORT).show()

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        val targetDevice = pairedDevices?.find { it.name == serverDeviceName }

        if (targetDevice != null) {
            Toast.makeText(this, "장치 찾음: $serverDeviceName, 연결 시도 중...", Toast.LENGTH_SHORT).show()
            connectToDevice(targetDevice)
        } else {
            Toast.makeText(this, "페어링된 장치에서 '$serverDeviceName'를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Thread {
            var socket: BluetoothSocket? = null
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread {
                        Toast.makeText(this, "BLUETOOTH_CONNECT 권한이 없습니다.", Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }

                runOnUiThread { Toast.makeText(this, "장치와 연결 중...", Toast.LENGTH_SHORT).show() }
                socket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()

                runOnUiThread { Toast.makeText(this, "연결 성공! 메시지 전송 중...", Toast.LENGTH_SHORT).show() }
                val outputStream: OutputStream = socket.outputStream
                outputStream.write("Hello from Android!".toByteArray())

                val inputStream = socket.inputStream
                val buffer = ByteArray(1024)
                val bytesRead = inputStream.read(buffer)
                val response = String(buffer, 0, bytesRead)

                runOnUiThread { Toast.makeText(this, "서버 응답: $response", Toast.LENGTH_LONG).show() }
            } catch (e: IOException) {
                runOnUiThread { Toast.makeText(this, "연결 실패: ${e.message}", Toast.LENGTH_LONG).show() }
                e.printStackTrace()
            } finally {
                try {
                    socket?.close()
                    runOnUiThread { Toast.makeText(this, "연결 종료", Toast.LENGTH_SHORT).show() }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }
}