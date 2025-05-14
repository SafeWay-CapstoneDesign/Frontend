package com.example.safeway

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
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
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import org.w3c.dom.Document
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


class LocationShareFragment : Fragment() {
    private lateinit var view:View

    private lateinit var searchLocationLauncher: ActivityResultLauncher<Intent>
    //학교의 위도와 경도 값(최초 위치 세팅용)
    private val schoolLatitude:Double = 37.374528
    private val schoolLongitude:Double = 126.633608
    private lateinit var location: Location


    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private lateinit var tMapGps: TMapGpsManager
    private var routePoints: MutableList<TMapPoint> = mutableListOf()
    private var turnTypes: MutableList<Int> = mutableListOf()

    private var role: String ="undefined"  //로그인한 유저의 역할 정보 저장. star면 위치를 post하고, parent면 위치를 get한다

    private var lastTurnType:String = "null"




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

        // SharedPreferences에서 role 가져오기
        role = requireContext().getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
            .getString("role", null).toString()
        Log.d("role 불러오기", "불러온 role: $role")

        if (role == "GUARDIAN") {
            val checkbox: CheckBox = view.findViewById(R.id.checkBox)
            checkbox.visibility = View.GONE
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


    // 마지막으로 위치를 보낸 시간 (timestamp)
    // 이 시간을 기반으로 일정 시간 간격으로 현재 위치를 서버에 보내거나 받음
    private var lastPostedTime = 0L

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                // 위치 보정 적용
                location = locationResult.lastLocation?.let { originalLocation ->
                    Location(originalLocation).apply {
                        latitude = originalLocation.latitude - 0.00005
                        longitude = originalLocation.longitude - 0.000025
                    }
                } ?: return

                val userLatitude = location.latitude
                val userLongitude = location.longitude

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastPostedTime >= 10_000) {
                    if (role == "STAR") {
                        val chkbox = view.findViewById<CheckBox>(R.id.checkBox)
                        if (chkbox.isChecked) {
                            postLocationToServer(userLatitude, userLongitude)
                        }
                    } else if (role == "GUARDIAN") {
                        getConnectedLocationFromServer()
                    }
                    lastPostedTime = currentTime
                }

                showCurrentLocationOnMap(userLatitude, userLongitude)
                onLocationChange(location)
            }
        }
    }

    //서버로 현재 위치 정보를 보내는 함수
    private fun postLocationToServer(latitude: Double, longitude: Double) {
        val token = requireContext()
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("accessToken", null)

        val accessToken = "Bearer $token"

        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient()

        // 현재 시간을 "HH:mm:ss" 형식으로 변환
        val currentTime = getCurrentTime()

        val json = JSONObject().apply {
            put("startName", "출발지")  // 필요한 경우 실제 출발지명으로 변경
            put("endName", "도착지")    // 필요한 경우 실제 도착지명으로 변경
            put("latitude", latitude)
            put("longitude", longitude)
            put("tdistance", 0)         // 필요 시 거리 계산해서 넣어도 됩니다
            put("ttime", currentTime)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://3.39.8.9:8080/location") // 실제 서버 URL로 수정
            .post(requestBody)
            .addHeader("accept", "*/*")
            .addHeader("Authorization", accessToken)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("LocationPost", "위치 전송 성공, ${json}")
                } else {
                    Log.e("LocationPost", "위치 전송 실패: ${response.code}")
                }
            }
        })
    }

    //서버로부터 지팡이 사용자의 위치를 받아오는 함수
    private fun getConnectedLocationFromServer() {
        val token = requireContext()
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("accessToken", null)

        val accessToken = "Bearer $token"

        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("http://3.39.8.9:8080/location/connected")  // 서버에서 위치 정보를 가져오는 엔드포인트
            .get()
            .addHeader("accept", "application/json")
            .addHeader("Authorization", accessToken)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "서버 통신 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        try {
                            val jsonObject = JSONObject(responseBody)
                            val latitude = jsonObject.getDouble("latitude")
                            val longitude = jsonObject.getDouble("longitude")
                            val time = jsonObject.optString("ttime", "")

                            Log.d("LocationGet", "위치 수신 성공: ($latitude, $longitude) at $time")

                            requireActivity().runOnUiThread {
                                showStarLocationOnMap(latitude, longitude)
                                Toast.makeText(requireContext(), "연결된 사용자 위치 수신 완료", Toast.LENGTH_SHORT).show()
                            }

                        } catch (e: Exception) {
                            Log.e("LocationGet", "JSON 파싱 오류: ${e.message}")
                        }
                    }
                } else {
                    Log.e("LocationGet", "위치 수신 실패: ${response.code}")
                }
            }
        })
    }


    // 현재 시간을 "HH:mm:ss" 형식으로 반환하는 함수
    private fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val date = Date() // 현재 시간
        return dateFormat.format(date)
    }








    @SuppressLint("ClickableViewAccessibility")
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

        tMapView.setOnTouchListener { _, _ ->
            lastUserInteractionTime = System.currentTimeMillis()
            false  // 이벤트 전달을 막지 않음
        }

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

    // GPS 위치값 변경 시 호출되는 함수
    fun onLocationChange(location: Location) {
        val currentPoint = TMapPoint(location.latitude, location.longitude)
//        val currentPoint = TMapPoint(37.38578078194957, 126.63981250078893)

        Log.d("currentTMapPoint : ", currentPoint.toString())
        checkTurnType(currentPoint)
    }



    // 경로 검색 후 지도에 띄워주고, 내부적으로 계산할 경로 정보들을 저장해두는 함수
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

        //경로를 검색하여 지도에 표시
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

        //경로를 검색하여 경로 정보를 저장
        tMapData.findPathDataAllType(
            TMapData.TMapPathType.PEDESTRIAN_PATH,
            tMapPointStart,
            tMapPointEnd,
            object : TMapData.FindPathDataAllListenerCallback {

                override fun onFindPathDataAll(document: Document?) {
                    // 기존 데이터 초기화
                    routePoints.clear()
                    turnTypes.clear()

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

    private var isFirstLocationUpdate = true
    private var lastUserInteractionTime = System.currentTimeMillis()


    //현재 위치를 지도에 마커로 표시해주는 함수
    private fun showCurrentLocationOnMap(latitude: Double, longitude: Double) {
        if (!isAdded || context == null) return  // 🔐 context 붙어있는지 확인
        Log.d("현재 위치", "latitude: $latitude, longitude: $longitude")

        val linearLayoutTmap = view?.findViewById<LinearLayout>(R.id.linearLayoutTmap)
        val tMapView = linearLayoutTmap?.getChildAt(0) as? TMapView ?: return

        if (role == "STAR") {
            val currentTime = System.currentTimeMillis()
            if (isFirstLocationUpdate || currentTime - lastUserInteractionTime > 5_000) {
                tMapView.setCenterPoint(longitude, latitude)
                isFirstLocationUpdate = false
            }
        }

        val markerItem = TMapMarkerItem()
        val tMapPoint = TMapPoint(latitude, longitude)
        markerItem.tMapPoint = tMapPoint
        markerItem.name = "현재 위치"

        // 안전한 context 접근
        context?.let {
            val bitmap = BitmapFactory.decodeResource(it.resources, R.drawable.curlocation)
            markerItem.icon = Bitmap.createScaledBitmap(bitmap, 80, 80, false)
        }

        markerItem.setPosition(0.5f, 1.0f)
        tMapView.addMarkerItem("currentLocation", markerItem)
    }


    //star의 위치를 지도에 마커로 표시해주는 함수
    private fun showStarLocationOnMap(latitude: Double, longitude: Double) {
        Log.d("star의 위치", "latitude: $latitude, longitude: $longitude")

        val linearLayoutTmap = view?.findViewById<LinearLayout>(R.id.linearLayoutTmap)
        val tMapView = linearLayoutTmap?.getChildAt(0) as? TMapView ?: return

        val currentTime = System.currentTimeMillis()

        // 중심 이동 조건: 최초 1회 또는 사용자의 지도 조작이 5초 이상 없을 경우
        if (isFirstLocationUpdate || currentTime - lastUserInteractionTime > 5_000) {
            tMapView.setCenterPoint(longitude, latitude)
            isFirstLocationUpdate = false
        }

        val markerItem = TMapMarkerItem()
        val tMapPoint = TMapPoint(latitude, longitude)
        markerItem.tMapPoint = tMapPoint
        markerItem.name = "star 위치"

        val bitmap = BitmapFactory.decodeResource(requireContext().resources, R.drawable.star)
        markerItem.icon = Bitmap.createScaledBitmap(bitmap, 60, 80, false)

        markerItem.setPosition(0.5f, 1.0f)
        tMapView.addMarkerItem("starLocation", markerItem)
    }


    // 현재 위치와 가장 가까운 경로 포인트를 찾고, turnType을 확인하여 Toast 메시지 출력
    private fun checkTurnType(currentPoint: TMapPoint) {
        Log.d("checkTurnType - currentPoint", currentPoint.toString())

        if (routePoints.isEmpty()) return

        var minDistance = Double.MAX_VALUE
        var closestIndex = -1

        for (i in routePoints.indices) {
            val point = routePoints[i]
            val distance = getDistance(currentPoint, point)
            Log.d("checkTurnType - 계산포인트", "$currentPoint, $point")
            Log.d("checkTurnType - 거리", distance.toString())

            if (distance < minDistance) {
                minDistance = distance
                closestIndex = i
            }
        }

        if (closestIndex != -1 && minDistance < 10.0) {
            val turnType = turnTypes[closestIndex]
            val turnMessage = getTurnMessage(turnType)
            Log.d("checkTurnType - 가장 가까운 포인트", "index: $closestIndex, 거리: $minDistance, 메시지: $turnMessage")

            val textView10: TextView = view.findViewById(R.id.textView10)

            if (turnMessage.isNotEmpty() && lastTurnType != turnType.toString()) {
                textView10.text = turnMessage
                lastTurnType = turnType.toString()

                val turnInfoMessage = "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}, turnMessage, $turnType"
                Toast.makeText(requireContext(), "turnType 다름. $turnInfoMessage 전달 시도", Toast.LENGTH_SHORT).show()
                Log.d("turnInfoMessage", turnInfoMessage)

                BluetoothManager.sendMessage(turnInfoMessage, { response ->
                    requireActivity().runOnUiThread {
                        // 응답 처리
                    }
                }, { error ->
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                    }
                })
            }

        } else {
            // 가까운 포인트가 없거나 너무 멀면 직진 안내
            val textView10: TextView = view.findViewById(R.id.textView10)
            textView10.text = "직진하세요"
            lastTurnType = "11"
        }
    }

    // 두 좌표 간 거리 계산하는 함수
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
