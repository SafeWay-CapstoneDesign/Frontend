package com.example.safeway

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.safeway.databinding.FragmentHomeConnectedBinding

class FragmentHomeConnected : Fragment() {

    private var deviceName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            deviceName = it.getString("deviceName") // 'deviceName'을 받음
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Fragment의 레이아웃을 inflate
        val binding = FragmentHomeConnectedBinding.inflate(inflater, container, false)

        // deviceName을 TextView에 표시
        binding.deviceName.text = deviceName ?: "기기 없음" // 기기 이름 표시

//        // testEnter 버튼 클릭 시 TestActivity로 이동
//        binding.testEnter.setOnClickListener {
//            // TestActivity로 이동하는 인텐트 생성
//            val intent = Intent(requireContext(), TestBluetoothMessageActivity::class.java)
//            startActivity(intent) // 액티비티 시작
//        }

        return binding.root
    }

    companion object {
        // 새로운 인스턴스를 생성하는 팩토리 메서드
        @JvmStatic
        fun newInstance(deviceName: String) =
            FragmentHomeConnected().apply {
                arguments = Bundle().apply {
                    putString("deviceName", deviceName) // deviceName을 전달
                }
            }
    }
}
