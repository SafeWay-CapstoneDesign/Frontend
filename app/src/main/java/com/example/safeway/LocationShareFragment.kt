package com.example.safeway

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.skt.Tmap.TMapData
import com.skt.Tmap.TMapMarkerItem
import com.skt.Tmap.TMapPoint
import com.skt.Tmap.TMapView
import org.w3c.dom.Document


class LocationShareFragment : Fragment() {
    private lateinit var view:View

    private lateinit var searchLocationLauncher: ActivityResultLauncher<Intent>
    private val schoolLatitude:Double = 37.374528
    private val schoolLongitude:Double = 126.633608

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    //뷰가 생성될 때 실행되는 코드
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        view = inflater.inflate(R.layout.fragment_location_share, container, false)

        // 장소 검색 버튼 클릭
        val addButton: TextView = view.findViewById(R.id.destinationTextView)
        addButton.setOnClickListener {
            val intent = Intent(requireContext(), SearchLocationActivity::class.java)
            searchLocationLauncher.launch(intent)
        }

        // ActivityResultLauncher 초기화
        searchLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
                val longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
                val locationName = data?.getStringExtra("locationName") ?: "선택한 위치"

                // 선택된 위치까지의 경로를 검색해서 화면에 보여줌
                searchRoute(latitude, longitude, locationName)
            }
        }

        return view

    }

    //뷰가 생성된 후에 실행되는 코드
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //tmap뷰 초기화(api인증 등)
        initializeTmapView(view)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        getCurrentLocation()


    }

    private fun initializeTmapView(view:View) {

        val linearLayoutTmap = view.findViewById<LinearLayout>(R.id.linearLayoutTmap)
        val tMapView = TMapView(requireContext())
        linearLayoutTmap.addView(tMapView)

        // 🚀 API 키 설정
        val apiKey = "qsfeCUetAU2xKy85eyxNi4qtFWq2h3Uo1EjiIvR1"
        tMapView.setSKTMapApiKey(apiKey)

        // "확대" 버튼 클릭
        val buttonZoomIn: Button = view.findViewById(R.id.buttonZoomIn)
        buttonZoomIn.setOnClickListener { tMapView.MapZoomIn() }

        // "축소" 버튼 클릭
        val buttonZoomOut: Button = view.findViewById(R.id.buttonZoomOut)
        buttonZoomOut.setOnClickListener { tMapView.MapZoomOut() }

        // 지도 중심 학교로 설정
        tMapView.setCenterPoint(schoolLongitude, schoolLatitude)
        tMapView.zoomLevel = 17

    }




    private fun searchRoute(latitude: Double, longitude: Double, locationName: String) {
        // 🚗 경로 표시 (API 인증 후 실행)
        val linearLayoutTmap = view?.findViewById<LinearLayout>(R.id.linearLayoutTmap)
        val tMapView = linearLayoutTmap?.getChildAt(0) as? TMapView // 기존 TMapView 가져오기

        // API 키 설정
        val apiKey = "qsfeCUetAU2xKy85eyxNi4qtFWq2h3Uo1EjiIvR1"
        tMapView?.setSKTMapApiKey(apiKey)

        val tMapData = TMapData()
        val tMapPointStart = TMapPoint(schoolLatitude, schoolLongitude)   //학교 7호관 좌표
        val tMapPointEnd = TMapPoint(latitude, longitude)
        Log.d("받아온 좌표정보", latitude.toString())

        tMapData.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, tMapPointStart, tMapPointEnd) { tMapPolyLine ->

            if (tMapPolyLine != null) {
                Log.d("LocationShareFragment", "경로 추가 시작")

                tMapPolyLine.lineColor = Color.BLUE
                tMapPolyLine.lineWidth = 2f
                tMapView?.addTMapPolyLine("Line1", tMapPolyLine)
                Log.d("LocationShareFragment", "경로 추가 완료")

                // 지도 중심 및 줌 설정
                tMapView?.setCenterPoint(schoolLongitude, schoolLatitude)
                tMapView?.zoomLevel = 17

                // 마커 추가
                val markerItem = TMapMarkerItem()
                val tMapPoint = TMapPoint(latitude, longitude)
                markerItem.tMapPoint = tMapPoint
                markerItem.name = locationName

                val bitmap = BitmapFactory.decodeResource(requireContext().resources, R.drawable.map_pin)
                markerItem.icon = Bitmap.createScaledBitmap(bitmap, 100, 100, false)

                markerItem.setPosition(0.5f, 1.0f)
                tMapView?.addMarkerItem("selectedLocation", markerItem)

                //목적지 textview 업데이트
                val addButton: TextView = view.findViewById(R.id.destinationTextView)
                addButton.setText(locationName)

            } else {
                Log.e("LocationShareFragment", "경로 데이터를 가져오지 못했습니다.")
            }
        }

        tMapData.findPathDataAllType(
            TMapData.TMapPathType.PEDESTRIAN_PATH,
            tMapPointStart,
            tMapPointEnd,
            object : TMapData.FindPathDataAllListenerCallback {
                override fun onFindPathDataAll(document: Document?) {
                    document?.documentElement?.getElementsByTagName("Placemark")?.let { nodeListPlacemark ->
                        for (i in 0 until nodeListPlacemark.length) {
                            val nodeListPlacemarkItem = nodeListPlacemark.item(i).childNodes
                            var descriptionText = ""
                            var geometryType = ""
                            var coordinates = ""

                            for (j in 0 until nodeListPlacemarkItem.length) {
                                val item = nodeListPlacemarkItem.item(j)

                                when (item.nodeName) {
                                    "description" -> {
                                        descriptionText = item.textContent.trim()
                                    }
                                    "Point", "LineString" -> { // geometry 정보 추출
                                        geometryType = item.nodeName
                                        val coordNode = item.childNodes
                                        for (k in 0 until coordNode.length) {
                                            if (coordNode.item(k).nodeName == "coordinates") {
                                                coordinates = coordNode.item(k).textContent.trim()
                                            }
                                        }
                                    }
                                }
                            }

                            // 로그 출력
                            Log.d("TMap Debug", "Description: $descriptionText")
                            Log.d("TMap Debug", "Geometry Type: $geometryType")
                            Log.d("TMap Debug", "Coordinates: $coordinates")
                        }
                    }
                }
            }
        )


    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            // 위치 권한 요청
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                100
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val userLatitude = it.latitude
                val userLongitude = it.longitude

                // TMapView에 현재 위치 표시
                showCurrentLocationOnMap(userLatitude, userLongitude)
            }
        }
    }

    private fun showCurrentLocationOnMap(latitude: Double, longitude: Double) {
        val linearLayoutTmap = view?.findViewById<LinearLayout>(R.id.linearLayoutTmap)
        val tMapView = linearLayoutTmap?.getChildAt(0) as? TMapView

        tMapView?.setCenterPoint(longitude, latitude)

        val markerItem = TMapMarkerItem()
        val tMapPoint = TMapPoint(latitude, longitude)
        markerItem.tMapPoint = tMapPoint
        markerItem.name = "현재 위치"

        val bitmap = BitmapFactory.decodeResource(requireContext().resources, R.drawable.map_pin)
        markerItem.icon = Bitmap.createScaledBitmap(bitmap, 100, 100, false)

        markerItem.setPosition(0.5f, 1.0f)
        tMapView?.addMarkerItem("currentLocation", markerItem)
    }


}
