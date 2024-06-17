package com.example.USGcap

import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

//import com.google.firebase.database.ValueEventListener
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import org.w3c.dom.Text


class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    private lateinit var webView: WebView
    private lateinit var db: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val h = findViewById<TextView>(R.id.humidityTextView)
        val t = findViewById<TextView>(R.id.temperatureTextView)
        val p = findViewById<TextView>(R.id.pumpTextView)

        // Firestore 초기화
        db = Firebase.firestore

        // 특정 컬렉션의 특정 문서의 특정 필드 가져오기
        // DETECT
        val detectionResultsRef = db.collection("detection_results")
            .document("latest_detection")

//        detectionResultsRef.get()
//            .addOnSuccessListener { document ->
//                if (document != null) {
//                    val confidence = document.getDouble("confidence")
//                    Log.d("MainActivity", "confidence: $confidence")
//                } else {
//                    Log.d("MainActivity", "No such document")
//                }
//            }
//            .addOnFailureListener { exception ->
//                Log.d("MainActivity", "get failed with ", exception)
//            }

        detectionResultsRef.addSnapshotListener { documentSnapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                val confidence = documentSnapshot.getDouble("confidence")
                Log.d(TAG, "SNAP confidence: $confidence")

                val formattedConfidence = String.format("%.2f", confidence)
                h.text = "현재 정확도: $formattedConfidence"
            } else {
                Log.d(TAG, "No such document")
            }
        }

        // TEMP
        val temperatureReadingRef = db.collection("temperature_reading")
            .document("latest_reading")

//        temperatureReadingRef.get()
//            .addOnSuccessListener { document ->
//                if (document != null) {
//                    val ambientTemperature = document.getDouble("ambient_temperature")
//                    val objectTemperature = document.getDouble("object_temperature")
//                    Log.d("MainActivity", "ambient_temperature: $ambientTemperature")
//                    Log.d("MainActivity", "object_temperature: $objectTemperature")
//                } else {
//                    Log.d("MainActivity", "No such document")
//                }
//            }
//            .addOnFailureListener { exception ->
//                Log.d("MainActivity", "get failed with ", exception)
//            }

        temperatureReadingRef.addSnapshotListener { documentSnapshot, e ->
            if (e != null) {
                Log.w("MainActivity", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                val ambientTemperature = documentSnapshot.getDouble("ambient_temperature")
                val objectTemperature = documentSnapshot.getDouble("object_temperature")

                Log.d(TAG, "SNAP ambient_temperature: $ambientTemperature")
                Log.d(TAG, "SNAP object_temperature: $objectTemperature")

                val formattedTemperature = String.format("%.2f", ambientTemperature)
                t.text = "현재 온도: $formattedTemperature °C"
            } else {
                Log.d("MainActivity", "No such document")
            }
        }

        // PUMP
        val noticeTextView = findViewById<TextView>(R.id.noticeTextView)
        val pumpBtn = findViewById<Button>(R.id.pumpBtn)
        val operationBtn = findViewById<TextView>(R.id.operationBtn)
        var pump: Boolean = false

        val pumpRef = db.collection("pump_state")
            .document("latest_value")

        // 초기 pump_state 가져오기
        pumpRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    pump = document.getBoolean("pump_state") ?:false
                    Log.d("MainActivity", "pump: $pump")
                    p.text = "펌프 상태: $pump"

                    // pump_state 값에 따라 레이아웃 변경
                    if (pump) {
                        noticeTextView.text = "주의! 화재 발생"
                        pumpBtn.text = "끄기"
                        operationBtn.visibility = View.VISIBLE
                    } else {
                        noticeTextView.text = ""
                        pumpBtn.text = "켜기"
                        operationBtn.visibility = View.GONE
                    }
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }

        // pump_state 갱신 시 가져오기
        pumpRef.addSnapshotListener { documentSnapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                pump = documentSnapshot.getBoolean("pump_state") ?:false
                Log.d(TAG, "SNAP pump: $pump")
                p.text = "펌프 상태: $pump"

                // pump_state 값에 따라 레이아웃 변경
                if (pump) {
                    noticeTextView.text = "주의! 화재 발생"
                    pumpBtn.text = "끄기"
                    operationBtn.visibility = View.VISIBLE
                } else {
                    noticeTextView.text = ""
                    pumpBtn.text = "켜기"
                    operationBtn.visibility = View.GONE
                }
            } else {
                Log.d(TAG, "No such document")
            }
        }

        // 버튼을 클릭하면 펌프 끄기
        val otherBtn = findViewById<Button>(R.id.pumpBtn)
        otherBtn.setOnClickListener {
            if (pump) {
                turnOffPump()
            } else {
                turnOnPump()
            }
        }

        // webView 설정
        webView = findViewById(R.id.webView)
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.builtInZoomControls = true
        webView.settings.setSupportZoom(true)

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()

        val videoUrl = "http://192.168.0.17:8000/stream.mjpg"
        //val videoUrl = "https://raspcam.megidisk.synology.me/"

        webView.loadUrl(videoUrl)

    }

    // 펌프를 끄는 함수
    private fun turnOffPump() {
        val docRef = db.collection("pump_state")
            .document("latest_value")

        // false로 업데이트
        docRef.update("pump_state", false)
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot successfully updated!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error updating document", e)
            }
    }

    // 펌프를 켜는 함수
    private fun turnOnPump() {
        val docRef = db.collection("pump_state")
            .document("latest_value")

        // true로 업데이트
        docRef.update("pump_state", true)
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot successfully updated!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error updating document", e)
            }
    }

}