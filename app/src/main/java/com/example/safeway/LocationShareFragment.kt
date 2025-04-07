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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.skt.Tmap.TMapData
import com.skt.Tmap.TMapGpsManager
import com.skt.Tmap.TMapMarkerItem
import com.skt.Tmap.TMapPoint
import com.skt.Tmap.TMapView
import org.w3c.dom.Document
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


class LocationShareFragment : Fragment() {
    private lateinit var view:View

    private lateinit var searchLocationLauncher: ActivityResultLauncher<Intent>
    private val schoolLatitude:Double = 37.374528
    private val schoolLongitude:Double = 126.633608
    private lateinit var location: Location


    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private lateinit var tMapGps: TMapGpsManager
    private var routePoints: MutableList<TMapPoint> = mutableListOf()
    private var turnTypes: MutableList<Int> = mutableListOf()





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
        createLocationRequest()
        createLocationCallback()
        getCurrentLocation()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 1000 // 1초마다 위치 업데이트
            fastestInterval = 1000 // 가장 빠른 업데이트 간격 (1초)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

    }


    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                location = locationResult.lastLocation!!
                if (location != null) {
                    val userLatitude = location.latitude
                    val userLongitude = location.longitude

                    // 실체 위치 오차에 대한 보정값 (왼쪽 아래 방향)
                    val correctionLat = 0.0001
                    val correctionLng = 0.0001
                    val correctedLatitude = userLatitude - correctionLat
                    val correctedLongitude = userLongitude - correctionLng

                    // 지도에 보정된 위치 표시
                    showCurrentLocationOnMap(correctedLatitude, correctedLongitude)

                    // 보정된 Location 객체 생성
                    val correctedLocation = Location(location).apply {
                        latitude = correctedLatitude
                        longitude = correctedLongitude
                    }

                    // 보정된 Location 전달
                    onLocationChange(correctedLocation)
                }
            }
        }
    }




    private fun initializeTmapView(view:View) {

        val linearLayoutTmap = view.findViewById<LinearLayout>(R.id.linearLayoutTmap)
        val tMapView = TMapView(requireContext())
        linearLayoutTmap.addView(tMapView)

        // 🚀 API 키 설정
        val apiKey = "qsfeCUetAU2xKy85eyxNi4qtFWq2h3Uo1EjiIvR1"
        tMapView.setSKTMapApiKey(apiKey)

        initGps()


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

    private fun initGps() {
        tMapGps = TMapGpsManager(requireContext()).apply {
            minTime = 1000 // 1초마다 갱신
            minDistance = 5f // 5m 이동 시 업데이트
            provider = TMapGpsManager.GPS_PROVIDER
            setLocationCallback()
        }
        tMapGps.OpenGps()
    }

    // GPS 변경 시 호출
    fun onLocationChange(location: Location) {
        val currentPoint = TMapPoint(location.latitude, location.longitude)
//        val currentPoint = TMapPoint(37.38578078194957, 126.63981250078893)

        Log.d("currentTMapPoint : ", currentPoint.toString())
        checkTurnType(currentPoint)
    }




    private fun searchRoute(latitude: Double, longitude: Double, locationName: String) {
        // 🚗 경로 표시 (API 인증 후 실행)
        val linearLayoutTmap = view?.findViewById<LinearLayout>(R.id.linearLayoutTmap)
        val tMapView = linearLayoutTmap?.getChildAt(0) as? TMapView // 기존 TMapView 가져오기

        // API 키 설정
        val apiKey = "qsfeCUetAU2xKy85eyxNi4qtFWq2h3Uo1EjiIvR1"
        tMapView?.setSKTMapApiKey(apiKey)

        val tMapData = TMapData()
        val tMapPointStart = TMapPoint(location.latitude, location.longitude)   //학교 7호관 좌표
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
                            var turnType: Int? = null  // Int?로 선언해서 null 여부 판단 가능하게 함

                            for (j in 0 until nodeListPlacemarkItem.length) {
                                val item = nodeListPlacemarkItem.item(j)

                                when (item.nodeName) {
                                    "description" -> {
                                        descriptionText = item.textContent.trim()
                                    }
                                    "Point", "LineString" -> {
                                        geometryType = item.nodeName
                                        val coordNode = item.childNodes

                                        for (k in 0 until coordNode.length) {
                                            if (coordNode.item(k).nodeName == "coordinates") {
                                                coordinates = coordNode.item(k).textContent.trim()
                                            }
                                        }
                                    }
                                    "tmap:turnType" -> {
                                        turnType = item.childNodes.item(0).textContent.trim().toInt()
                                    }
                                }
                            }

                            // turnType이 있을 때만 routePoints에 좌표 추가
                            if (turnType != null && coordinates.isNotEmpty()) {
                                val parsedCoordinates = coordinates.split(" ")
                                    .map { it.split(",").map { coord -> coord.toDouble() } }

                                if (parsedCoordinates.isNotEmpty()) {
                                    val firstCoordinate = parsedCoordinates[0]
                                    val tMapPoint = TMapPoint(firstCoordinate[1], firstCoordinate[0])
                                    routePoints.add(tMapPoint)
                                    turnTypes.add(turnType)

                                    Log.d("routePoint추가: ", routePoints.toString())
                                    Log.d("turnType추가 : ", turnTypes.toString())
                                }
                            }

                            Log.d("TMap Debug", "Description: $descriptionText, Geometry Type: $geometryType, Coordinates: $coordinates, turnType: $turnType")
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

        // 위치 업데이트 요청
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }



    private fun showCurrentLocationOnMap(latitude: Double, longitude: Double) {
        Log.d("현재 위치", "latitude: $latitude, longitude: $longitude")

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

    // 현재 위치와 가장 가까운 경로 포인트를 찾고, turnType을 확인하여 Toast 메시지 출력
    private fun checkTurnType(currentPoint: TMapPoint) {
        Log.d("checkTurnType - currentPoint", currentPoint.toString())

        Log.d("checkTurnType - 최종 포인트들 : ", routePoints.toString())
        Log.d("checkTurnType - 최종 턴타입들 : ", turnTypes.toString())
        for (i in routePoints.indices) {
            val point = routePoints[i]
            Log.d("checkTurnType - 계산포인트 : ", "${currentPoint.toString()}, ${point.toString()}")
            Log.d("checkTurnType - 거리 : ", getDistance(currentPoint, point).toString())
            val textView10: TextView = view.findViewById(R.id.textView10)
            if (getDistance(currentPoint, point) < 10.0) { // 10m 이내 도착 시
//                Toast.makeText(requireContext(), "turnpoint에 접근하였습니다.", Toast.LENGTH_SHORT).show()
                val turnMessage = getTurnMessage(turnTypes[i])
                Log.d("checkTurnType ", "turnpoint에 접근하였습니다., ${turnTypes[i]}")
                if (turnMessage.isNotEmpty()) {
                    textView10.setText(turnMessage)
//                    Toast.makeText(requireContext(), turnMessage, Toast.LENGTH_SHORT).show()
                }
                break
            }else{
                textView10.setText("직진하세요")

            }
        }
    }

    // 두 좌표 간 거리 계산
    private fun getDistance(p1: TMapPoint, p2: TMapPoint): Double {
        val lat1 = Math.toRadians(p1.latitude)
        val lon1 = Math.toRadians(p1.longitude)
        val lat2 = Math.toRadians(p2.latitude)
        val lon2 = Math.toRadians(p2.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        // Haversine formula
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val radius = 6371000 // Earth radius in meters
        return radius * c
    }


    // turnType에 따른 메시지 변환
    private fun getTurnMessage(turnType: Int): String {
        return when (turnType) {
            in 1..7 -> "안내 없음"
            11 -> "직진하세요"
            12 -> "좌회전하세요"
            13 -> "우회전하세요"
            14 -> "U턴하세요"
            16 -> "8시 방향으로 좌회전하세요"
            17 -> "10시 방향으로 좌회전하세요"
            18 -> "2시 방향으로 우회전하세요"
            19 -> "4시 방향으로 우회전하세요"
            184 -> "경유지입니다"
            185 -> "첫 번째 경유지입니다"
            186 -> "두 번째 경유지입니다"
            187 -> "세 번째 경유지입니다"
            188 -> "네 번째 경유지입니다"
            189 -> "다섯 번째 경유지입니다"
            125 -> "육교를 이용하세요"
            126 -> "지하보도를 이용하세요"
            127 -> "계단으로 진입하세요"
            128 -> "경사로로 진입하세요"
            129 -> "계단과 경사로로 진입하세요"
            200 -> "출발지입니다"
            201 -> "목적지에 도착했습니다"
            211 -> "횡단보도를 건너세요"
            212 -> "좌측 횡단보도를 건너세요"
            213 -> "우측 횡단보도를 건너세요"
            214 -> "8시 방향 횡단보도를 건너세요"
            215 -> "10시 방향 횡단보도를 건너세요"
            216 -> "2시 방향 횡단보도를 건너세요"
            217 -> "4시 방향 횡단보도를 건너세요"
            218 -> "엘리베이터를 이용하세요"
            233 -> "직진하세요 (임시)"
            else -> "알 수 없는 경로"
        }
    }



}
