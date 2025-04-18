package com.example.safeway

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

class EmergencyContactActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emergencyContactAdapter: EmergencyAdapter

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

        fetchEmergencyContacts()
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

    private fun parseEmergencyContacts(responseBody: String?): List<EmergencyContact> {
        val contacts = mutableListOf<EmergencyContact>()
        if (responseBody != null) {
            try {
                val jsonArray = JSONArray(responseBody)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val name = jsonObject.getString("ename")
                    val phone = jsonObject.getString("ephone")
                    contacts.add(EmergencyContact(name, phone))
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
            val intent = Intent().apply {
                putExtra("contactName", selectedContact.name)
                putExtra("phoneNumber", selectedContact.phoneNumber)
            }
            setResult(RESULT_OK, intent)
            finish()
        }

        recyclerView.adapter = emergencyContactAdapter
    }

}
