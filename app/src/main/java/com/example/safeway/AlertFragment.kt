package com.example.safeway

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Date

class AlertFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var alertAdapter: AlertAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_alert, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView(view)
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val dummyAlerts = listOf(
            Alert("보호자 연동", "횃불이님이 보호자 연동을 요청하였어요.", Date(System.currentTimeMillis() - 5 * 60 * 1000)),  // 5분 전
            Alert("위치 공유", "김기수님이 목적지에 잘 도착했어요.", Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000)),  // 2시간 전
            Alert("공지사항", "비밀번호를 변경하신지 6개월이 지났어요.\n보안을 위해 비밀번호를 변경하세요", Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000)) // 3일 전
        )

        alertAdapter = AlertAdapter(dummyAlerts) { selectedAlert ->
            // 클릭 이벤트 추가 가능
        }

        recyclerView.adapter = alertAdapter
    }
}
