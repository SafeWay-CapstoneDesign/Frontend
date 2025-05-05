package com.example.safeway

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import java.io.IOException
import java.util.UUID

class FindingDeviceFragment : Fragment() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val serverDeviceName = "raspberrypi"
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null
    private var buttonSend: Button? = null

    private var retryCount = 0
    private val maxRetries = 5
    private val retryDelayMillis = 5000L

    private var isTryingToConnect = true
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_finding_device, container, false)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buttonSend = view.findViewById(R.id.buttonSend)
        buttonSend?.isEnabled = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissions()
        } else {
            initBluetoothConnection()
        }
    }

    override fun onPause() {
        super.onPause()
        isTryingToConnect = false
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )

        val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                if (results.all { it.value }) {
                    initBluetoothConnection()
                } else {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "블루투스 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                    }
                }
            }

        permissionLauncher.launch(permissions)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun initBluetoothConnection() {
        if (bluetoothAdapter == null) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            bluetoothAdapter.enable()
            if (isAdded) {
                Toast.makeText(requireContext(), "블루투스를 활성화합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        if (isAdded) {
            Toast.makeText(requireContext(), "페어링된 장치를 검색 중...", Toast.LENGTH_SHORT).show()
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        val targetDevice = pairedDevices?.find { it.name == serverDeviceName }

        if (targetDevice != null) {
            if (isAdded) {
                Toast.makeText(requireContext(), "장치 찾음: $serverDeviceName, 연결 시도 중...", Toast.LENGTH_SHORT).show()
            }
            connectToDevice(targetDevice)
        } else {
            if (isAdded) {
                Toast.makeText(requireContext(), "페어링된 장치에서 '$serverDeviceName'를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Thread {
            if (!isTryingToConnect) return@Thread

            try {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    if (isAdded) {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "BLUETOOTH_CONNECT 권한이 없습니다.", Toast.LENGTH_LONG).show()
                        }
                    }
                    return@Thread
                }

                if (isAdded) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "장치와 연결 중...", Toast.LENGTH_SHORT).show()
                    }
                }

                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                BluetoothManager.socket = bluetoothSocket

                if (isAdded) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "연결 성공!", Toast.LENGTH_SHORT).show()
                        buttonSend?.isEnabled = true

                        val bundle = Bundle().apply {
                            putString("deviceName", serverDeviceName)
                        }

                        val fragment = FragmentHomeConnected().apply {
                            arguments = bundle
                        }

                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.main_container, fragment)
                            .commit()

                        (requireActivity() as MainActivity).updateToolbarTitle("연결된 기기")
                    }
                }

            } catch (e: IOException) {
                e.printStackTrace()

                retryCount++
                if (isAdded) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "연결 실패 (${retryCount}/$maxRetries): ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                if (retryCount < maxRetries && isTryingToConnect) {
                    handler.postDelayed({
                        connectToDevice(device)
                    }, retryDelayMillis)
                } else if (isAdded) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "연결 재시도 횟수를 초과했습니다.", Toast.LENGTH_LONG).show()
                        (requireActivity() as MainActivity).supportFragmentManager.beginTransaction()
                            .replace(R.id.main_container, HomeFragment())
                            .commit()
                        (requireActivity() as MainActivity).updateToolbarTitle("SafeWay")
                    }
                }
            }
        }.start()
    }
}
