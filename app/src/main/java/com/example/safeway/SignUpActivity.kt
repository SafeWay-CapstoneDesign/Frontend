package com.example.safeway

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.safeway.model.User
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

class SignUpActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val gson = Gson()

    private lateinit var usernameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var passwordConfirmInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var signUpBtn: Button
    private lateinit var progressDialog: ProgressDialog
    private lateinit var guardianCheckBox: CheckBox


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        usernameInput = findViewById(R.id.textInputUsername)
        emailInput = findViewById(R.id.textInputEmail)
        passwordInput = findViewById(R.id.textInputPassword)
        passwordConfirmInput = findViewById(R.id.textInputPasswordConfirm)
        phoneInput = findViewById(R.id.textInputPhone)
        signUpBtn = findViewById(R.id.SignUpBtn)
        guardianCheckBox = findViewById(R.id.checkboxGuardian)


        progressDialog = ProgressDialog(this).apply {
            setMessage("회원가입 중...")
            setCancelable(false)
        }

        signUpBtn.setOnClickListener {
            if (validateInput()) {
                val role = if (guardianCheckBox.isChecked) "GUARDIAN" else "STAR"

                val user = User(
                    username = usernameInput.text.toString(),
                    email = emailInput.text.toString(),
                    password = passwordInput.text.toString(),
                    phone = phoneInput.text.toString(),
                    role = role
                )

                sendSignUpRequest(user)
            }
        }
    }

    private fun validateInput(): Boolean {
        val username = usernameInput.text.toString()
        val email = emailInput.text.toString()
        val password = passwordInput.text.toString()
        val passwordConfirm = passwordConfirmInput.text.toString()
        val phone = phoneInput.text.toString()

        if (username.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
            Toast.makeText(this, "모든 항목을 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "유효한 이메일 형식이 아닙니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 4) {
            Toast.makeText(this, "비밀번호는 최소 4자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password != passwordConfirm) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!phone.matches(Regex("^01[016789]-?\\d{3,4}-?\\d{4}\$"))) {
            Toast.makeText(this, "올바른 전화번호를 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun sendSignUpRequest(user: User) {
        progressDialog.show()

        val json = gson.toJson(user)
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json)

        val request = Request.Builder()
            .url("http://3.39.8.9:8080/api/v1/auth")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this@SignUpActivity, "회원가입 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    progressDialog.dismiss()
                    if (response.isSuccessful) {
                        val token = response.body?.string() ?: ""

                        // 토큰 SharedPreferences에 저장
                        val sharedPref = getSharedPreferences("auth", MODE_PRIVATE)
                        sharedPref.edit().putString("accessToken", token).apply()

                        Toast.makeText(this@SignUpActivity, "회원가입 성공", Toast.LENGTH_SHORT).show()
                        finish() // ✅ 현재 SignUpActivity 종료 (즉, 로그인 화면으로 자동 복귀)
                    }
 else {
                        Toast.makeText(this@SignUpActivity, "회원가입 실패: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }


        })
    }
}
