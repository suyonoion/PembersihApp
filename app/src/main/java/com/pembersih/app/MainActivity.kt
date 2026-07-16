package com.pembersih.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // Registrasi Komponen Mesin
    private lateinit var listApps: ListView
    private lateinit var btnCleanJunk: Button
    private lateinit var activityManager: ActivityManager
    private var packageList = mutableListOf<String>()
    private var displayList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Pengkabelan Antarmuka Visual ke Mesin Logika
        listApps = findViewById(R.id.listApps)
        btnCleanJunk = findViewById(R.id.btnCleanJunk)
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        // Penyatuan Tuas Utama
        btnCleanJunk.setOnClickListener {
            eksekusiPembersihanTersembunyiOtomatis()
        }

        muatDaftarAplikasi()
    }

    private fun muatDaftarAplikasi() {
        val pm: PackageManager = packageManager
        val packages: List<ApplicationInfo> = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        packageList.clear()
        displayList.clear()

        // Sabuk konveyor: Saring aplikasi sistem inti
        for (appInfo in packages) {
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                val appName = pm.getApplicationLabel(appInfo).toString()
                displayList.add(appName)
                packageList.add(appInfo.packageName)
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listApps.adapter = adapter

        // Tuas Pemutus RAM (Tutup Paksa Manual)
        listApps.setOnItemClickListener { _, _, position, _ ->
            val targetPackage = packageList[position]
            activityManager.killBackgroundProcesses(targetPackage)
            Toast.makeText(this, "Proses $targetPackage dihentikan paksa.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun eksekusiPembersihanTersembunyiOtomatis() {
        if (!cekIzinRobotik()) {
            Toast.makeText(this, "Akses Lengan Robotik belum diaktifkan.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            return
        }

        // ALIRKAN LISTRIK: Aktifkan kopling mesin Lengan Robotik
        LenganRobotikService.koplingAktif = true
        Toast.makeText(this, "Mesin beroperasi. JANGAN sentuh layar.", Toast.LENGTH_LONG).show()
        
        val handler = Handler(Looper.getMainLooper())
        var jedaWaktu = 0L
        val intervalMesin = 3500L // Ruang eksekusi mekanis per aplikasi

        // Lontarkan aplikasi ke layar untuk dieksekusi sensor
        for (paketApp in packageList) {
            handler.postDelayed({
                bukaPengaturanAplikasi(paketApp)
            }, jedaWaktu)
            
            jedaWaktu += intervalMesin 
        }

        // PROTOKOL PENGHENTIAN PABRIK
        handler.postDelayed({
            // PUTUS ARUS: Matikan sensor robotik
            LenganRobotikService.koplingAktif = false 
            
            // Tarik paksa layar Panel Kendali kembali ke permukaan perangkat keras
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            
            Toast.makeText(this, "Siklus Pabrik Selesai. Lengan Robotik Dimatikan.", Toast.LENGTH_LONG).show()
        }, jedaWaktu)
    }

    private fun bukaPengaturanAplikasi(namaPaket: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$namaPaket")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun cekIzinRobotik(): Boolean {
        val prefString = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return prefString?.contains(packageName) == true
    }
}
