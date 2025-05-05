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

        val client = OkHttpClient()

        val token = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("accessToken", null)

        Log.d("토큰 불러오기", "불러온 토큰: $token")

        if (token == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        val request = Request.Builder()
            .url("http://3.39.8.9:8080/user")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 403) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "로그인 정보가 만료되었습니다.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val responseData = response.body?.string()
                    Log.d("서버 응답", "응답 데이터: $responseData")
                }
            }
        })


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
