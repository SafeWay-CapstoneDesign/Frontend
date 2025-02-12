package com.example.safeway

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button


class MypageFragment : Fragment() {

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mypage, container, false)

        // 버튼 클릭 이벤트 처리
        val addButton: Button = view.findViewById(R.id.guardian_interlock_btn)
        addButton.setOnClickListener {
            val intent = Intent(requireContext(), GuardianInterlockActivity::class.java)
            startActivity(intent)
        }

        return view
    }

}