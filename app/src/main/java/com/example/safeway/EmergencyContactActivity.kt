package com.example.safeway

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EmergencyContactActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emergencyContactAdapter: EmergencyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_emergency_contact)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // 새 방식으로 뒤로가기 동작 호출
        }

        setupRecyclerView()
    }

    // 더미 데이터를 추가하여 리사이클러뷰에 표시
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val dummyContacts = listOf(
            EmergencyContact("김철수", "010-1234-5678"),
            EmergencyContact("이영희", "010-2345-6789"),
            EmergencyContact("박민수", "010-3456-7890"),
            EmergencyContact("최지연", "010-4567-8901")
        )

        emergencyContactAdapter = EmergencyAdapter(dummyContacts) { selectedContact ->
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
