package com.example.safeway

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class LoginActivity : AppCompatActivity() {

    // OkHttpClient 인스턴스 생성
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        val token = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("accessToken", null)

        runOnUiThread {
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }

        if (token != null) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://3.39.8.9:8080/user")
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "자동 로그인 실패: 네트워크 오류", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        // 유효하지 않은 토큰이므로 무시하고 로그인 화면 유지
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "토큰이 만료되어 재로그인이 필요합니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }

        // ✅ 토큰이 있는 경우 MainActivity로 바로 이동

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)

        val loginButton = findViewById<Button>(R.id.LoginBtn)
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 모두 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginWithOkHttp(email, password)
        }

        val signupEnterBtn = findViewById<Button>(R.id.SignupEnterBtn)
        signupEnterBtn.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
            startActivity(intent)
        }

    }

    private fun loginWithOkHttp(email: String, password: String) {
        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("http://3.39.8.9:8080/api/v1/auth/login")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        val token = responseBody ?: ""

                        // SharedPreferences에 토큰 저장
                        val sharedPref = getSharedPreferences("auth", MODE_PRIVATE)
                        sharedPref.edit().putString("accessToken", token).apply()

                        getUserInfo(token)

                        Toast.makeText(this@LoginActivity, "로그인 성공", Toast.LENGTH_SHORT).show()

                        // 다음 화면으로 이동 처리
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()

                    } else {
                        Toast.makeText(this@LoginActivity, "로그인 실패: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        })
    }

    //유저 정보를 불러와서 sharedpref에 저장하는 함수
    private fun getUserInfo(token: String) {
        val request = Request.Builder()
            .url("http://3.39.8.9:8080/user") // 실제 API 엔드포인트
            .addHeader("Authorization", "Bearer $token") // 토큰 추가
            .build()

        // 비동기 요청
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "서버 요청 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    try {
                        // 사용자 정보 파싱
                        val user = JSONObject(jsonResponse)
                        val role = user.getString("role")

                        // SharedPreferences에 role 저장
                        val sharedPref = getSharedPreferences("auth", MODE_PRIVATE)
                        sharedPref.edit().putString("role", role).apply()

                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "응답 파싱 오류", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "응답 오류: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
