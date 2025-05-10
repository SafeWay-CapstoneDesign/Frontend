package com.example.safeway

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.sqrt

class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private var gyroSensor: Sensor? = null

    private var fallDetected = false
    private var lastFallTimestamp: Long = 0

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        accelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        showToast("✅ 낙상 감지 서비스 시작됨")
        Log.d("FallDetection", "서비스 시작됨")
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> processAccelerometer(event.values)
            Sensor.TYPE_GYROSCOPE -> processGyroscope(event.values)
        }
    }

    private var gyroRotationMagnitude = 0f

    private fun processGyroscope(values: FloatArray) {
        val rotX = values[0]
        val rotY = values[1]
        val rotZ = values[2]
        gyroRotationMagnitude = sqrt(rotX * rotX + rotY * rotY + rotZ * rotZ)
    }

    private fun processAccelerometer(values: FloatArray) {
        val accX = values[0]
        val accY = values[1]
        val accZ = values[2]
        val acceleration = sqrt(accX * accX + accY * accY + accZ * accZ)

        val currentTime = System.currentTimeMillis()

        //가속도 값 acceleration > 25일 때 사람이 넘어지거나 큰 충격을 받았다고 판단
        if (acceleration > 25) {
            lastFallTimestamp = currentTime
            fallDetected = true
            Log.d("FallDetection", "충격 감지됨: $acceleration")
            onFallDetected()
            fallDetected = false


        }

        //강한 충격이 발생한 후 1~5초 이내에 가속도 값이 2 이상이고 자이로 회전량이 0.5f 이하면
        //낙상으로 판단
        if (fallDetected && currentTime - lastFallTimestamp in 1000..5000) {
            if (acceleration < 2 && gyroRotationMagnitude < 0.5f) {
                Log.d("FallDetection", "낙상 감지됨 🚨")
                onFallDetected()
                fallDetected = false
            }
        }

        if (fallDetected && currentTime - lastFallTimestamp > 5000) {
            fallDetected = false
        }
    }

    private fun onFallDetected() {
        showToast("🚨 낙상 감지됨! 보호자에게 문자 전송 중...")
        sendEmergencySms()
    }

    private fun sendEmergencySms() {
        val sharedPrefs = getSharedPreferences("emergency", MODE_PRIVATE)
        val contactsJson = sharedPrefs.getString("contactNumbers", null)
        val type = object : TypeToken<List<String>>() {}.type
        val numbers: List<String> = if (contactsJson != null) {
            Gson().fromJson(contactsJson, type)
        } else emptyList()

        Log.d("numbers", numbers.toString())
        if (numbers.isEmpty()) {
            showToast("🚨 저장된 비상 연락처가 없습니다.")
            return
        }


        // 위치 정보 요청
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            showToast("위치 권한이 필요합니다.")
            return
        }

        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location == null) {
            Log.e("FallDetection", "❗ 위치 정보를 가져올 수 없습니다. 문자 전송 생략됨.")
            return
        }


        val message = if (location != null) {
            val lat = location.latitude
            val lon = location.longitude
            "낙상이 감지되었습니다. 사용자 상태를 확인해주세요.\n위치: https://maps.google.com/?q=$lat,$lon"
        } else {
            "낙상이 감지되었습니다. 사용자 상태를 확인해주세요. (위치 정보 없음)"
        }

        val smsManager = SmsManager.getDefault()
        val parts = smsManager.divideMessage(message)
        for (number in numbers) {
            smsManager.sendMultipartTextMessage(number, null, parts, null, null)
            Log.d("FallDetection", "📤 $number 에게 문자 전송 완료")
        }

        showToast("비상 연락처로 문자 전송 완료")
    }




    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Log.d("FallDetection", "서비스 종료됨")
    }
}
