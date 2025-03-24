package com.example.safeway

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safeway.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // 연결하고자 하는 특정 기기 이름
    private val targetDeviceName = "raspberrypi"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Toolbar를 ActionBar로 설정
        setSupportActionBar(binding.toolbar)

        // 시스템 바 인셋 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setBottomNavigationView()

        // 앱 초기 실행 시 홈화면으로 설정
        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.fragment_home
        }

        // 블루투스 연결 상태 확인
        checkBluetoothConnection()
    }

    private fun setBottomNavigationView() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.fragment_home -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_container, HomeFragment()).commit()
                    supportActionBar?.title = "SafeWay"
                    checkBluetoothConnection()

                    true

                }
                R.id.fragment_share_location -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_container, LocationShareFragment()).commit()
                    supportActionBar?.title = "위치 및 길안내"
                    true
                }
                R.id.fragment_alert -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_container, AlertFragment()).commit()
                    supportActionBar?.title = "알림"
                    true
                }
                R.id.fragment_mypage -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_container, MypageFragment()).commit()
                    supportActionBar?.title = "마이페이지"
                    true
                }
                else -> false
            }
        }
    }

    // 블루투스 연결 상태 확인
    private fun checkBluetoothConnection() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "블루투스를 지원하지 않는 장치입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 블루투스가 활성화 되어 있지 않으면 활성화 요청
        if (!bluetoothAdapter.isEnabled) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            bluetoothAdapter.enable()
        }

        // 페어링된 기기 목록을 확인하여 특정 기기 연결 여부 확인
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        for (device in pairedDevices) {
            if (device.name == targetDeviceName) {
                // 특정 기기가 연결되었을 때, connected 프래그먼트 표시
//                showFindingFragment()
                showConnectedFragment()
                return
            }
        }

        // 특정 기기가 연결되지 않은 상태면 기본 화면 표시
        showHomeFragment()

    }

    // 특정 기기가 연결된 경우, connected 프래그먼트 표시
    private fun showConnectedFragment() {
        // Bundle로 targetDeviceName 전달
        val bundle = Bundle().apply {
            putString("deviceName", targetDeviceName)
        }

        val fragment = FragmentHomeConnected().apply {
            arguments = bundle
        }
        supportFragmentManager.beginTransaction().replace(R.id.main_container, fragment).commit()
        supportActionBar?.title = "연결된 기기"
    }

    // 기본 HomeFragment 표시
    private fun showHomeFragment() {
        supportFragmentManager.beginTransaction().replace(R.id.main_container, HomeFragment()).commit()
        supportActionBar?.title = "SafeWay"
    }

    private fun showFindingFragment(){
        supportFragmentManager.beginTransaction().replace(R.id.main_container,
            FindingDeviceFragment()).commit()
        supportActionBar?.title = "기기 검색 중"
    }



}
