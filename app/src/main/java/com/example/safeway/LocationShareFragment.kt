package com.example.safeway

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.skt.Tmap.TMapMarkerItem
import com.skt.Tmap.TMapPoint
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



        //마커 생성
        val markerItem1 = TMapMarkerItem()
        val tMapPoint1 = TMapPoint( 37.374528, 126.633608) // SKT타워
        val bitmap = BitmapFactory.decodeResource(requireContext().resources, R.drawable.map_pin) // 원본 마커 아이콘
        if (bitmap != null) {
            // 마커 아이콘 크기 조정
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false) // 원하는 크기로 조정 (예: 100x100)

            markerItem1.icon = scaledBitmap // 크기 조정된 비트맵 아이콘 설정
        } else {
            // 비트맵 로드 실패 시 처리
            Log.e("LocationShareFragment", "Bitmap load failed!")
        }
        markerItem1.setPosition(0.5f , 1.0f) // 마커의 중심점을 중앙, 하단으로 설정
        markerItem1.tMapPoint = tMapPoint1 // 마커의 좌표 지정
        markerItem1.name = "SKT타워" // 마커의 타이틀 지정
        tMapView.addMarkerItem("markerItem1", markerItem1) // 지도에 마커 추가







    }
}
