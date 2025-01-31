package com.example.safeway

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safeway.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

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
    }

    fun setBottomNavigationView() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.fragment_home -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_container, HomeFragment()).commit()
                    supportActionBar?.title = "SafeWay" // toolbar 텍스트 변경
                    true
                }
                R.id.fragment_share_location -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_container, LocationShareFragment()).commit()
                    supportActionBar?.title = "위치 및 길안내" // toolbar 텍스트 변경
                    true
                }
                R.id.fragment_alert -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_container, AlertFragment()).commit()
                    supportActionBar?.title = "알림" // toolbar 텍스트 변경
                    true
                }
                R.id.fragment_mypage -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_container, MypageFragment()).commit()
                    supportActionBar?.title = "마이페이지" // toolbar 텍스트 변경
                    true
                }
                else -> false
            }
        }
    }


}
