package com.example.safeway

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AddDeviceActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BluetoothDeviceAdapter
    private val deviceList = mutableListOf<BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_device)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val cancelBtn: Button = findViewById(R.id.cancelButton)
        cancelBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val rescanBtn: Button = findViewById(R.id.rescanButton)
        rescanBtn.setOnClickListener {
            restartDiscovery()
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BluetoothDeviceAdapter(deviceList)
        recyclerView.adapter = adapter

        adapter.setOnItemClickListener { device ->
            pairDevice(device)
        }

        resetAndSearchBluetooth()
    }

    private fun resetAndSearchBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter ?: return

        requestBluetoothPermissions()
        startDiscovery()
    }

    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val granted = permissions.values.all { it }
                if (!granted) {
                    Toast.makeText(this, "블루투스 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        permissionLauncher.launch(permissions)
    }

    private fun startDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        bluetoothAdapter.startDiscovery()

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)

        val bondFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(bondStateReceiver, bondFilter)
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        val deviceName = it.name
                        if (deviceName != null && !"Unknown".equals(deviceName, ignoreCase = true)) {
                            if (!deviceList.contains(it)) {
                                deviceList.add(it)
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    private val bondStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)

                    when (bondState) {
                        BluetoothDevice.BOND_BONDING -> {
                            Toast.makeText(this@AddDeviceActivity, "페어링 중...", Toast.LENGTH_SHORT).show()
                        }
                        BluetoothDevice.BOND_BONDED -> {
                            Toast.makeText(this@AddDeviceActivity, "페어링 완료: ${device?.name}", Toast.LENGTH_SHORT).show()

                            // 페어링 성공 후 HomeActivity로 이동
                            val intent = Intent(this@AddDeviceActivity, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            finish() // 현재 Activity 종료

                        }
                        BluetoothDevice.BOND_NONE -> {
                            Toast.makeText(this@AddDeviceActivity, "페어링 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun pairDevice(device: BluetoothDevice) {
        if (device.bondState == BluetoothDevice.BOND_NONE) {
            Toast.makeText(this, "페어링 요청: ${device.name}", Toast.LENGTH_SHORT).show()
            device.createBond()
        } else {
            Toast.makeText(this, "이미 페어링된 기기입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restartDiscovery() {
        deviceList.clear()
        adapter.notifyDataSetChanged()
        startDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        unregisterReceiver(bondStateReceiver)
    }
}
