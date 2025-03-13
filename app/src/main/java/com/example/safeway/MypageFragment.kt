package com.example.safeway

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment


class MypageFragment : Fragment() {

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mypage, container, false)

        // 버튼 클릭 이벤트 처리
        val guardian_interlock_btn: Button = view.findViewById(R.id.guardian_interlock_Enterbtn)
        guardian_interlock_btn.setOnClickListener {
            val intent = Intent(requireContext(), GuardianInterlockActivity::class.java)
            startActivity(intent)
        }

        val myinfoeditBtn: Button = view.findViewById(R.id.myinfoeditBtn)
        myinfoeditBtn.setOnClickListener {
            val intent = Intent(requireContext(), MyInfoEditActivity::class.java)
            startActivity(intent)
        }

        val emergencyContactEnterBtn: Button = view.findViewById(R.id.emergencyContactEnterBtn)
        emergencyContactEnterBtn.setOnClickListener {
            val intent = Intent(requireContext(), EmergencyContactActivity::class.java)
            startActivity(intent)
        }

        return view
    }

}