package com.example.safeway

data class Location(
    val name: String,
    val address: String,
    val latitude: Double,  // 위도 추가
    val longitude: Double  // 경도 추가
)
