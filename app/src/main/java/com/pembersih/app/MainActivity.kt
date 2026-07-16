package com.pembersih.app

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Handler
import android.os.Looper


class MainActivity : AppCompatActivity() {

    private lateinit var listApps: ListView
    private lateinit var btnCleanJunk: Button
    private lateinit var activityManager: ActivityManager
    private var packageList = mutableListOf<String>()
    private var displayList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listApps = findViewById(R.id.listApps)
        btnCleanJunk = findViewById(R.id.btnCleanJunk)
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        btnCleanJunk.setOnClickListener { eksekusiPembersihanTersembunyiOtomatis()
        }
        
        muatDaftarAplikasi()
    }

        private fun eksekusiPembersihanTersembunyiOtomatis() {
        if (!cekIzinRobotik()) {
            Toast.makeText(this, "Akses Lengan Robotik (Aksesibilitas) belum diaktifkan.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            return
        }

        Toast.makeText(this, "Memulai otomasi. Jangan sentuh layar.", Toast.LENGTH_LONG).show()
        
        // Pindahkan daftar aplikasi target ke sabuk konveyor
        val handler = Handler(Looper.getMainLooper())
        var jedaWaktu = 0L

        for (paketApp in packageList) {
            handler.postDelayed({
                bukaPengaturanAplikasi(paketApp)
            }, jedaWaktu)
            
            // Beri waktu 3 detik per aplikasi agar Robotik bisa mengidentifikasi layar dan mengeklik
            jedaWaktu += 3000L 
        }
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


    private fun hapusDirektori(dir: File?): Long {
        var size = 0L
        if (dir != null && dir.isDirectory) {
            val children = dir.listFiles()
            if (children != null) {
                for (child in children) {
                    size += child.length()
                    child.delete()
                }
            }
        }
        return size
    }

    private fun muatDaftarAplikasi() {
        val pm: PackageManager = packageManager
        val packages: List<ApplicationInfo> = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        packageList.clear()
        displayList.clear()

        for (appInfo in packages) {
            // Saring sistem inti, tampilkan aplikasi yang dapat dikontrol
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                val appName = pm.getApplicationLabel(appInfo).toString()
                displayList.add(appName)
                packageList.add(appInfo.packageName)
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listApps.adapter = adapter

        listApps.setOnItemClickListener { _, _, position, _ ->
            val targetPackage = packageList[position]
            activityManager.killBackgroundProcesses(targetPackage)
            Toast.makeText(this, "Proses $targetPackage dihentikan paksa.", Toast.LENGTH_SHORT).show()
        }
    }
}
