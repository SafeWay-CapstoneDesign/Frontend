package com.example.safeway

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.skt.Tmap.TMapView


class LocationShareFragment : Fragment() {
    //뷰가 생성될 때 실행되는 코드
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

    //뷰가 생성된 후에 실행되는 코드
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //티맵 생성
        val linearLayoutTmap = view.findViewById<LinearLayout>(R.id.linearLayoutTmap)
        val tMapView = TMapView(requireContext())
        linearLayoutTmap.addView(tMapView)
        tMapView.setSKTMapApiKey("qsfeCUetAU2xKy85eyxNi4qtFWq2h3Uo1EjiIvR1")    //앱키

        tMapView.setCenterPoint(126.633608, 37.374528);
        tMapView.zoomLevel = 17;

        // "확대" 버튼 클릭
        val buttonZoomIn: Button = view.findViewById(R.id.buttonZoomIn)
        buttonZoomIn.setOnClickListener(View.OnClickListener { tMapView.MapZoomIn() })


        // "축소" 버튼 클릭
        val buttonZoomOut: Button = view.findViewById(R.id.buttonZoomOut)
        buttonZoomOut.setOnClickListener(View.OnClickListener { tMapView.MapZoomOut() })




    }
}
