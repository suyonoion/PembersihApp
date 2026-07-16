package com.pembersih.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class LenganRobotikService : AccessibilityService() {

    // Status Mesin (State Machine) untuk mengunci jalur perakitan
    private var statusOperasi = STATUS_MENCARI_PENYIMPANAN
    
    companion object {
        const val STATUS_MENCARI_PENYIMPANAN = 0
        const val STATUS_MENCARI_HAPUS_CACHE = 1
        const val STATUS_SELESAI_KEMBALI = 2
    }

    // Kepadatan Informasi: Matriks Kamus Universal (Substring)
    // Mesin akan mencari fragmen kata ini, bukan kalimat utuh.
    private val matriksPenyimpanan = arrayOf(
        "penyimpanan", "storage", "memori", "memory", "ruang"
    )
    
    private val matriksHapusCache = arrayOf(
        "cache", "memori sementara", "berkas sampah", "residu"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Hanya proses jika ada perubahan arsitektur layar
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && 
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }

        val akarNode = rootInActiveWindow ?: return

        when (statusOperasi) {
            STATUS_MENCARI_PENYIMPANAN -> {
                if (pemindaiMendalam(akarNode, matriksPenyimpanan, true)) {
                    statusOperasi = STATUS_MENCARI_HAPUS_CACHE
                }
            }
            STATUS_MENCARI_HAPUS_CACHE -> {
                if (pemindaiMendalam(akarNode, matriksHapusCache, true)) {
                    statusOperasi = STATUS_SELESAI_KEMBALI
                    tarikMundurLengan()
                }
            }
        }
    }

    // Algoritma Pemindai Node Rekursif
    // Membongkar setiap lapis UI dari luar ke dalam
    private fun pemindaiMendalam(node: AccessibilityNodeInfo?, targetMatriks: Array<String>, eksekusiKlik: Boolean): Boolean {
        if (node == null) return false

        val teksNode = node.text?.toString()?.lowercase() ?: ""
        val deskripsiNode = node.contentDescription?.toString()?.lowercase() ?: ""

        // Cocokkan pola fragmen kata
        var cocok = false
        for (kataKunci in targetMatriks) {
            if (teksNode.contains(kataKunci) || deskripsiNode.contains(kataKunci)) {
                cocok = true
                break
            }
        }

        // Jika pola ditemukan dan komponen memiliki engsel (bisa diklik)
        if (cocok) {
            if (node.isClickable) {
                if (eksekusiKlik) node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            } else if (node.parent != null && node.parent.isClickable) {
                // Pabrikan sering meletakkan fungsi klik pada wadah (parent), bukan pada teksnya
                if (eksekusiKlik) node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }

        // Jika tidak ditemukan di lapisan ini, bor ke lapisan berikutnya
        for (i in 0 until node.childCount) {
            if (pemindaiMendalam(node.getChild(i), targetMatriks, eksekusiKlik)) {
                return true
            }
        }

        return false
    }

    private fun tarikMundurLengan() {
        // Jeda mekanis absolut untuk memastikan proses I/O selesai sebelum mundur
        Thread.sleep(800)
        performGlobalAction(GLOBAL_ACTION_BACK)
        Thread.sleep(400)
        performGlobalAction(GLOBAL_ACTION_BACK)
        
        // Reset tuas status untuk aplikasi berikutnya di sabuk konveyor
        statusOperasi = STATUS_MENCARI_PENYIMPANAN
    }

    override fun onInterrupt() {
        // Pengawas sistem memutus koneksi, reset tuas
        statusOperasi = STATUS_MENCARI_PENYIMPANAN
    }
}
