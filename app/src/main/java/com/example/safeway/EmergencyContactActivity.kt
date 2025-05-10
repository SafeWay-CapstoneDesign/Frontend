package com.example.safeway

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class EmergencyContactActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emergencyContactAdapter: EmergencyAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_contact)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val addContactButton: Button = findViewById(R.id.button2)
        addContactButton.setOnClickListener {
            openContactPicker()
        }

        val connectedParent: LinearLayout = findViewById(R.id.ConnectedParent)
        connectedParent.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("보호자 연결 삭제")
                .setMessage("모든 보호자 연결을 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    deleteConnections()
                }
                .setNegativeButton("취소", null)
                .show()
        }


        fetchParentInfo()
        fetchEmergencyContacts()
    }

    //현재 로그인된 사용자의 모든 보호자 연결을 삭제
    private fun deleteConnections() {
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
            .url("http://3.39.8.9:8080/user-connections")
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@EmergencyContactActivity,
                        "연결 삭제에 실패했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@EmergencyContactActivity,
                            "모든 보호자 연결이 삭제되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@EmergencyContactActivity,
                            "삭제 실패: ${response.code}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }


    // 연락처 선택을 위한 ActivityResultLauncher
    private val contactPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val contactUri = data?.data
                if (contactUri != null) {
                    handleContactResult(contactUri)
                }
            }
        }

    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        contactPickerLauncher.launch(intent)
    }

    private fun handleContactResult(uri: Uri) {
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val hasPhoneNumberIndex =
                    it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                if (idIndex != -1 && nameIndex != -1 && hasPhoneNumberIndex != -1) {
                    val contactId = it.getString(idIndex)
                    val contactName = it.getString(nameIndex)
                    val hasPhoneNumber = it.getInt(hasPhoneNumberIndex)

                    if (hasPhoneNumber > 0) {
                        val phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )

                        phoneCursor?.use { pCursor ->
                            if (pCursor.moveToFirst()) {
                                val phoneIndex =
                                    pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (phoneIndex != -1) {
                                    val contactPhone = pCursor.getString(phoneIndex)
                                    addEmergencyContact(contactName, contactPhone)
                                    return
                                }
                            }
                        }

                        Log.e("EmergencyContact", "전화번호를 찾을 수 없습니다.")
                        Toast.makeText(this, "전화번호를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("EmergencyContact", "이 연락처에는 전화번호가 없습니다.")
                        Toast.makeText(this, "이 연락처에는 전화번호가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun addEmergencyContact(name: String, phone: String) {
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

        val json = JSONObject().apply {
            put("ename", name)
            put("ephone", phone)
        }

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )


        val request = Request.Builder()
            .url("http://3.39.8.9:8080/emergency")
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@EmergencyContactActivity,
                        "연락처 추가에 실패했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        fetchEmergencyContacts()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@EmergencyContactActivity,
                            "연락처 추가에 실패했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    //비상 연락처 리스트 fetch하는 함수
    private val emergencyPhoneNumbers = mutableListOf<String>()
    private fun fetchEmergencyContacts() {
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
            .url("http://3.39.8.9:8080/emergency")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@EmergencyContactActivity,
                        "데이터를 불러오는 데 실패했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val emergencyContacts = parseEmergencyContacts(responseBody)
                    Log.d("emergencyContacts", responseBody.toString())
                    // 전화번호만 저장
                    emergencyPhoneNumbers.clear()
                    emergencyContacts.forEach { contact ->
                        emergencyPhoneNumbers.add(contact.phoneNumber) // phoneNumber는 JSON 필드에 맞게 수정
                    }
                    val sharedPrefs = getSharedPreferences("emergency", MODE_PRIVATE).edit()
                    val jsonString = Gson().toJson(emergencyPhoneNumbers)
                    sharedPrefs.putString("contactNumbers", jsonString)
                    sharedPrefs.apply()

                    runOnUiThread {
                        setupRecyclerView(emergencyContacts)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@EmergencyContactActivity, "서버 오류", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        })
    }

    //star의 보호자를 조회하는 함수
    private fun fetchParentInfo() {
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
            .url("http://3.39.8.9:8080/user-connections/connections")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@EmergencyContactActivity,
                        "데이터를 불러오는 데 실패했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("fetchParentInfo", responseBody.toString())

                    val gson = Gson()
                    val listType = object : TypeToken<List<ConnectionInfo>>() {}.type
                    val connectionList: List<ConnectionInfo> = gson.fromJson(responseBody, listType)

                    runOnUiThread {
                        val rawDate = connectionList.firstOrNull()?.connectedAt
                        val formattedDate = rawDate?.let {
                            val parsedDate = OffsetDateTime.parse(it)
                            parsedDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))
                        } ?: "연결 정보 없음"

                        findViewById<TextView>(R.id.textViewConnectedAt).text = formattedDate
                        findViewById<TextView>(R.id.textViewParentName).text = connectionList.firstOrNull()?.guardianName
                    }

                } else {
                    runOnUiThread {
                        Toast.makeText(this@EmergencyContactActivity, "서버 오류", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        })
    }

    private fun parseEmergencyContacts(responseBody: String?): List<EmergencyContact> {
        val contacts = mutableListOf<EmergencyContact>()
        if (responseBody != null) {
            try {
                val jsonArray = JSONArray(responseBody)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val id = jsonObject.getString("id")
                    val name = jsonObject.getString("ename")
                    val phone = jsonObject.getString("ephone")
                    contacts.add(EmergencyContact(id, name, phone))
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return contacts
    }

    private fun setupRecyclerView(contactList: List<EmergencyContact>) {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        emergencyContactAdapter = EmergencyAdapter(this, contactList) { selectedContact ->
            // 여기에 삭제 API 연결
            deleteEmergencyContact(selectedContact.id)
        }

        recyclerView.adapter = emergencyContactAdapter
    }


    //연락처 삭제 api
    fun deleteEmergencyContact(id: String) {
        val client = OkHttpClient()
        val token = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("accessToken", null)

        if (token == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val request = Request.Builder()
            .url("http://3.39.8.9:8080/emergency/$id") // 이 API 경로가 맞는지 서버 문서 확인!
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EmergencyContactActivity, "삭제 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EmergencyContactActivity, "삭제 완료", Toast.LENGTH_SHORT).show()
                        fetchEmergencyContacts() // 삭제 후 리스트 갱신
                    } else {
                        Toast.makeText(this@EmergencyContactActivity, "삭제 실패: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }



}
