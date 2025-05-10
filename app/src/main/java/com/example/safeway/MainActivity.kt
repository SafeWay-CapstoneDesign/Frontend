package com.example.safeway

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safeway.databinding.ActivityMainBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val serverDeviceName = "raspberrypi"


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setBottomNavigationView()

        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.fragment_home
        }

        checkBluetoothConnection()

        
        //낙상 감지 서비스 실행
        //sms 권한 체크
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1)
        }

        val serviceIntent = Intent(this, FallDetectionService::class.java)
        startService(serviceIntent)
    }


    private fun setBottomNavigationView() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.fragment_home -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_container, HomeFragment()).commit()
                    binding.toolbarTitle.text = "SafeWay"
                    checkBluetoothConnection()

                    true

                }
                R.id.fragment_share_location -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_container, LocationShareFragment()).commit()
                    binding.toolbarTitle.text = "위치 및 길안내"
                    true
                }
                R.id.fragment_alert -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_container, AlertFragment()).commit()
                    binding.toolbarTitle.text = "알림"
                    true
                }
                R.id.fragment_mypage -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_container, MypageFragment()).commit()
                    binding.toolbarTitle.text = "마이페이지"
                    true
                }
                else -> false
            }
        }
    }

    // 블루투스 연결 상태 확인
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun checkBluetoothConnection() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "블루투스를 지원하지 않는 장치입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // 권한 요청이 필요한 경우 처리
                return
            }
            bluetoothAdapter.enable()
        }

        // ✅ 등록된(페어링된) 기기 목록 확인
        val bondedDevices = bluetoothAdapter.bondedDevices
        var deviceFound = false
        for (device in bondedDevices) {
            Log.d("페어링된 기기", "기기 이름: ${device.name}")
            if (device.name == serverDeviceName) {
                deviceFound = true
                break
            }
        }

        if (deviceFound) {
            showFindingFragment()
        } else {
            showHomeFragment()
        }
    }

//    // 특정 기기가 연결된 경우, connected 프래그먼트 표시
//    private fun showConnectedFragment() {
//        // Bundle로 targetDeviceName 전달
//        val bundle = Bundle().apply {
//            putString("deviceName", serverDeviceName)
//        }
//
//        val fragment = FragmentHomeConnected().apply {
//            arguments = bundle
//        }
//        supportFragmentManager.beginTransaction().replace(R.id.main_container, fragment).commit()
//        binding.toolbarTitle.text = "연결된 기기"
//    }

    // 기본 HomeFragment 표시
    private fun showHomeFragment() {
        supportFragmentManager.beginTransaction().replace(R.id.main_container, HomeFragment()).commit()
        binding.toolbarTitle.text = "SafeWay"
        updateToolbarTitle("SafeWay")
    }

    private fun showFindingFragment(){
        supportFragmentManager.beginTransaction().replace(R.id.main_container,
            FindingDeviceFragment()).commit()
//        binding.toolbarTitle.text = "기기 검색 중"
        updateToolbarTitle("기기 검색 중")
    }


    // MainActivity.kt
    fun updateToolbarTitle(title: String) {
        binding.toolbarTitle.text = title
    }




}
