package com.example.safeway

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.UUID

class TestBluetoothMessageActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val serverDeviceName = "raspberrypi"
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private lateinit var editTextInput: EditText
    private lateinit var buttonSend: Button
    private lateinit var textViewReceived: TextView

    private var bluetoothSocket: BluetoothSocket? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_bluetooth_message)

        // UI 요소 연결
        editTextInput = findViewById(R.id.editTextInput)
        buttonSend = findViewById(R.id.buttonSend)
        textViewReceived = findViewById(R.id.textViewReceived)

        buttonSend.setOnClickListener {
            val message = editTextInput.text.toString()
            if (message.isNotBlank()) {
                sendMessageOverBluetooth(message)
            } else {
                Toast.makeText(this, "보낼 메시지를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 블루투스 연결 시작
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
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread {
                        Toast.makeText(this, "BLUETOOTH_CONNECT 권한이 없습니다.", Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }

                runOnUiThread { Toast.makeText(this, "장치와 연결 중...", Toast.LENGTH_SHORT).show() }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()

                runOnUiThread {
                    Toast.makeText(this, "연결 성공! 메시지를 입력하세요.", Toast.LENGTH_SHORT).show()
                    buttonSend.isEnabled = true
                }

            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "연결 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }.start()
    }

    private fun sendMessageOverBluetooth(message: String) {
        Thread {
            try {
                val socket = bluetoothSocket
                if (socket != null && socket.isConnected) {
                    val outputStream = socket.outputStream
                    outputStream.write(message.toByteArray())

                    val inputStream = socket.inputStream
                    val buffer = ByteArray(1024)
                    val bytesRead = inputStream.read(buffer)
                    val response = String(buffer, 0, bytesRead)

                    runOnUiThread {
                        textViewReceived.text = "서버 응답: $response"
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "블루투스 연결이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "메시지 전송 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }.start()
    }
}
