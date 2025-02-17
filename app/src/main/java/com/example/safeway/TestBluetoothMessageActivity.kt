package com.example.safeway

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.UUID

class TestBluetoothMessageActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var sendButton: Button
    private lateinit var messageEditText: EditText
    private lateinit var bluetoothGatt: BluetoothGatt
    private var bluetoothAdapter: BluetoothAdapter? = null

    // GATT 특성 UUID (Python 코드와 일치시켜야 합니다)
    private val characteristicUuid: UUID = UUID.fromString("0000XXXX-0000-1000-8000-00805F9B34FB")  // Python 코드와 동일하게 변경

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_bluetooth_message)

        statusTextView = findViewById(R.id.statusTextView)
        sendButton = findViewById(R.id.sendButton)
        messageEditText = findViewById(R.id.messageEditText)

        // Bluetooth 어댑터 초기화
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            statusTextView.text = "Bluetooth를 지원하지 않는 기기입니다."
            return
        }

        if (bluetoothAdapter == null || bluetoothAdapter?.isEnabled != true) {
            statusTextView.text = "Bluetooth를 활성화하세요."
            return
        }

        // BLE 장치 연결
        connectToBluetoothDevice()

        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }
    }

    private fun connectToBluetoothDevice() {
        val deviceAddress = "A4:75:B9:BC:73:7D"  // Python 코드에서 사용하는 장치 주소로 변경
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)

        if (device != null) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 권한 요청 부분
                return
            }
            bluetoothGatt = device.connectGatt(this, false, gattCallback)
            statusTextView.text = "BLE 장치 연결 중..."
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(characteristicUuid)
                val characteristic = service.getCharacteristic(characteristicUuid)
                // 메시지 전송 준비
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val receivedMessage = characteristic.getStringValue(0)
                runOnUiThread {
                    // 메시지 수신 처리
                    statusTextView.text = "수신된 메시지: $receivedMessage"
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread {
                    statusTextView.text = "메시지 전송 성공"
                }
            } else {
                runOnUiThread {
                    statusTextView.text = "메시지 전송 실패"
                }
            }
        }
    }

    private fun sendMessage(messageText: String) {
        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(characteristicUuid)
            val characteristic = service.getCharacteristic(characteristicUuid)
            characteristic.setValue(messageText)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 권한 요청 부분
                return
            }
            gatt.writeCharacteristic(characteristic)
        }
    }
}
