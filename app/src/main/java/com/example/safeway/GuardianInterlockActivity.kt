package com.example.safeway

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class GuardianInterlockActivity : AppCompatActivity() {

    private var accessToken: String? = null
    private var guardianEmail: String? = null  // 보호자 이메일 저장

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_interlock)

        // 토큰 초기화
        val token = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("accessToken", null)
        accessToken = "Bearer $token"

        // 인셋 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 툴바 뒤로가기 버튼
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val addButton = findViewById<Button>(R.id.addButton)

        emailEditText.setOnEditorActionListener { _, actionId, event ->
            val isEnterPressed = (actionId == EditorInfo.IME_ACTION_SEARCH) ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)

            if (isEnterPressed) {
                val email = emailEditText.text.toString().trim()
                if (email.isNotEmpty()) {
                    checkGuardianExists(email)
                } else {
                    Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
                true  // 이벤트 소비
            } else {
                false
            }
        }


        addButton.setOnClickListener {
            if (guardianEmail != null) {
                connectGuardian(guardianEmail!!)
            } else {
                Toast.makeText(this, "보호자 이메일이 없습니다. 먼저 보호자를 검색해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkGuardianExists(email: String) {
        if (accessToken.isNullOrBlank()) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://3.39.8.9:8080/user-connections/guardian/check?email=$email")
            .addHeader("accept", "*/*")
            .addHeader("Authorization", accessToken!!)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@GuardianInterlockActivity, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    body?.let {
                        val json = JSONObject(it)
                        val exists = json.getBoolean("exists")

                        runOnUiThread {
                            val infoText = findViewById<TextView>(R.id.infoText)
                            val searchedUserLayout = findViewById<LinearLayout>(R.id.searchedUserLayout)
                            if (exists) {
                                val username = json.getString("username")
                                val role = json.getString("role")

                                guardianEmail = email // 이메일 저장

                                findViewById<TextView>(R.id.nameTextView).text = username
                                findViewById<TextView>(R.id.roleTextView).text = "($role)"
                                infoText.text = "보호자를 찾았습니다."
                                searchedUserLayout.visibility = View.VISIBLE
                            } else {
                                guardianEmail = null
                                infoText.text = "보호자를 찾을 수 없습니다."
                            }
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@GuardianInterlockActivity, "서버 응답 오류", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun connectGuardian(email: String) {
        if (accessToken.isNullOrBlank()) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient()

        val jsonBody = JSONObject()
        jsonBody.put("guardianEmail", email)

        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            jsonBody.toString()
        )

        val request = Request.Builder()
            .url("http://3.39.8.9:8080/user-connections")
            .post(requestBody)
            .addHeader("accept", "*/*")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", accessToken!!)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@GuardianInterlockActivity, "연결 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@GuardianInterlockActivity, "보호자 연결 성공!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@GuardianInterlockActivity, "보호자 연결 실패 (코드: ${response.code})", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
