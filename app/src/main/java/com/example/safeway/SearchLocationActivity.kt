package com.example.safeway

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.skt.Tmap.TMapData
import com.skt.Tmap.poi_item.TMapPOIItem

class SearchLocationActivity : AppCompatActivity() {
    // TextInputEditText 참조
    lateinit var inputLocation: TextInputEditText

    private lateinit var recyclerView: RecyclerView
    private lateinit var locationAdapter: LocationAdapter
    private var locationList: MutableList<Location> = mutableListOf() // 초기화 추가

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search_location)

        // inputLocation 초기화 위치를 setContentView 이후로 변경
        inputLocation = findViewById(R.id.inputLocation)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // 새 방식으로 뒤로가기 동작 호출
        }

        // 키보드에서 검색 버튼을 눌렀을 때 처리
        inputLocation.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation()

                // 키보드를 숨기기 (선택 사항)
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(inputLocation.windowToken, 0)

                true // 이벤트 소비
            } else {
                false
            }
        }
    }



    private fun searchLocation() {
        val tmapdata = TMapData()
        val strData = inputLocation.text.toString()
        locationList.clear()

        tmapdata.findAllPOI(strData, object : TMapData.FindAllPOIListenerCallback {
            @SuppressLint("NotifyDataSetChanged")
            override fun onFindAllPOI(poiItem: ArrayList<TMapPOIItem>?) {
                if (poiItem != null) {
                    for (item in poiItem) {
                        val latitude = item.poiPoint.latitude
                        val longitude = item.poiPoint.longitude

                        locationList.add(
                            Location(
                                name = item.poiName,
                                address = item.poiAddress.replace("null", ""),
                                latitude = latitude,
                                longitude = longitude
                            )
                        )

                        Log.d(
                            "POI Search",
                            "Name: ${item.poiName}, Address: ${item.poiAddress.replace("null", "")}, " +
                                    "Latitude: $latitude, Longitude: $longitude"
                        )
                    }

                    runOnUiThread {
                        locationAdapter.notifyDataSetChanged()
                        val infoText: TextView = findViewById(R.id.infoText)
                        infoText.visibility = View.GONE
                    }
                } else {
                    Log.e("TMap", "POI items are null or empty")
                }
            }
        })

        setupRecyclerView()
    }

    // 리사이클러뷰 초기화 및 어댑터 설정
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        locationAdapter = LocationAdapter(locationList) { selectedLocation ->
            val intent = Intent().apply {
                putExtra("latitude", selectedLocation.latitude)
                putExtra("longitude", selectedLocation.longitude)
                putExtra("locationName", selectedLocation.name)
            }
            setResult(RESULT_OK, intent)
            finish()
        }

        recyclerView.adapter = locationAdapter
    }

}
