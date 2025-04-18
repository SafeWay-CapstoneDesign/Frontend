package com.example.safeway

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safeway.model.User
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

class MyInfoEditActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText

    private lateinit var role: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_info_edit)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // EditText 초기화
        usernameEditText = findViewById(R.id.nameTextView)
        emailEditText = findViewById(R.id.emailTextView)
        phoneEditText = findViewById(R.id.phoneTextView)
        newPasswordEditText = findViewById(R.id.passwordTextView)
        confirmPasswordEditText = findViewById(R.id.passwordConfirmTextview)

        // 툴바 설정
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 취소 버튼 설정
        val cancelBtn: Button = findViewById(R.id.cancelButton)
        cancelBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 저장 버튼 설정
        val saveBtn: Button = findViewById(R.id.okButton)
        saveBtn.setOnClickListener {
            saveUserDetails()
        }

        // 사용자 정보 불러오기
        fetchUserDetails()
    }

    private fun fetchUserDetails() {
        val client = OkHttpClient()

        val token = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("accessToken", null)

        Log.d("토큰 불러오기", "불러온 토큰: $token")

        if (token == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = Request.Builder()
            .url("http://3.39.8.9:8080/user")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MyInfoEditActivity, "서버 요청 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    try {
                        val user = Gson().fromJson(jsonResponse, User::class.java)

                        runOnUiThread {
                            usernameEditText.setText(user.username)
                            emailEditText.setText(user.email)
                            phoneEditText.setText(user.phone)
                            role = user.role
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MyInfoEditActivity, "응답 파싱 오류", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MyInfoEditActivity, "응답 오류: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun saveUserDetails() {
        // 비밀번호 확인
        val newPassword = newPasswordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient()

        val token = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("accessToken", null)

        if (token == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedUser = User(
            username = usernameEditText.text.toString(),
            email = emailEditText.text.toString(),
            password = newPasswordEditText.text.toString(),
            phone = phoneEditText.text.toString(),
            role = role
        )

        // 비밀번호 변경이 있을 경우 추가
        val passwordUpdate = if (newPassword.isNotEmpty()) {
            "\"newPassword\": \"$newPassword\""
        } else {
            ""
        }

        val json = Gson().toJson(updatedUser) + if (passwordUpdate.isNotEmpty()) {
            ", $passwordUpdate"
        } else {
            ""
        }

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json)

        val request = Request.Builder()
            .url("http://3.39.8.9:8080/user")
            .addHeader("Authorization", "Bearer $token")
            .put(requestBody) // PUT 메서드로 정보 수정
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MyInfoEditActivity, "서버 요청 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MyInfoEditActivity, "정보가 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MyInfoEditActivity, "정보 저장 실패: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
