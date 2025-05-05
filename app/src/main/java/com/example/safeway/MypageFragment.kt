package com.example.safeway

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.safeway.model.User
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class MypageFragment : Fragment() {

    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
//    private lateinit var phoneTextView: TextView
    private lateinit var roleTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mypage, container, false)

        // TextView 객체 초기화
        usernameTextView = view.findViewById(R.id.textView12)
        emailTextView = view.findViewById(R.id.textView15)
//        phoneTextView = view.findViewById(R.id.textViewPhone)  // 전화번호 TextView
        roleTextView = view.findViewById(R.id.roleTextView)    // 역할 TextView

        // 서버에서 사용자 정보 조회
        fetchUserDetails()

        // 버튼 클릭 이벤트 처리
        val guardianInterlockBtn: Button = view.findViewById(R.id.guardian_interlock_Enterbtn)
        guardianInterlockBtn.setOnClickListener {
            val intent = Intent(requireContext(), GuardianInterlockActivity::class.java)
            startActivity(intent)
        }

        val myInfoEditBtn: Button = view.findViewById(R.id.myinfoeditBtn)
        myInfoEditBtn.setOnClickListener {
            val intent = Intent(requireContext(), MyInfoEditActivity::class.java)
            startActivity(intent)
        }

        val emergencyContactEnterBtn: Button = view.findViewById(R.id.emergencyContactEnterBtn)
        emergencyContactEnterBtn.setOnClickListener {
            val intent = Intent(requireContext(), EmergencyContactActivity::class.java)
            startActivity(intent)
        }

        val connectionManagementEnterBtn: Button = view.findViewById(R.id.connectionManagementEnterBtn)
        connectionManagementEnterBtn.setOnClickListener {
            val intent = Intent(requireContext(), ConnectionManageActivity::class.java)
            startActivity(intent)
        }

        val LogoutBtn: Button = view.findViewById(R.id.logoutBtn)
        LogoutBtn.setOnClickListener {
            // 로그아웃 확인 다이얼로그
            AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃하시겠어요?")
                .setPositiveButton("예") { _, _ ->
                    // SharedPreferences에서 토큰 삭제
                    val prefs = requireContext().getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
                    prefs.edit().remove("accessToken").apply()

                    // 로그인 화면으로 이동
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("아니오", null)
                .show()
        }





        return view
    }

    private fun fetchUserDetails() {
        val client = OkHttpClient()

        // SharedPreferences에서 accessToken 가져오기
        val token = requireContext().getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
            .getString("accessToken", null)
        Log.d("토큰 불러오기", "불러온 토큰: $token")


        if (token == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 사용자 정보 조회 API URL
        val request = Request.Builder()
            .url("http://3.39.8.9:8080/user") // 실제 API 엔드포인트
            .addHeader("Authorization", "Bearer $token") // 토큰 추가
            .build()

        // 비동기 요청
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "서버 요청 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    try {
                        val user = Gson().fromJson(jsonResponse, User::class.java)

                        requireActivity().runOnUiThread {
                            usernameTextView.text = user.username
                            emailTextView.text = user.email
//                            phoneTextView.text = user.phone
                            roleTextView.text = user.role
                        }
                    } catch (e: Exception) {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "응답 파싱 오류", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "응답 오류: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // JSON 응답에서 User 객체로 파싱
    private fun parseUser(response: String?): User {
        // Gson을 사용하여 JSON 응답을 User 객체로 변환
        return Gson().fromJson(response, User::class.java)
    }
}
