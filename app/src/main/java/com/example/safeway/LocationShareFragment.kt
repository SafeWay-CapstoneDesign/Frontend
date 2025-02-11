package com.example.safeway

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

class LocationShareFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_location_share, container, false)

        // 버튼 클릭 이벤트 처리
        val addButton: TextView = view.findViewById(R.id.textView9)
        addButton.setOnClickListener {
            val intent = Intent(requireContext(), SearchLocationActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}
