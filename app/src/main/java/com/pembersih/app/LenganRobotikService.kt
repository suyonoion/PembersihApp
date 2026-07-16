package com.pembersih.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class LenganRobotikService : AccessibilityService() {

    private var statusOperasi = STATUS_MENCARI_PENYIMPANAN
    
    // Blok Tuas Kopling Penghubung
    companion object {
        const val STATUS_MENCARI_PENYIMPANAN = 0
        const val STATUS_MENCARI_HAPUS_CACHE = 1
        const val STATUS_SELESAI_KEMBALI = 2
        
        // Parameter absolut pengunci arus. 
        // Secara default (saat hape menyala), lengan robotik tertidur.
        var koplingAktif = false 
    }

    private val matriksPenyimpanan = arrayOf(
        "penyimpanan", "storage", "memori", "memory", "ruang"
    )
    
    private val matriksHapusCache = arrayOf(
        "cache", "memori sementara", "berkas sampah", "residu"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // PEMUTUS ARUS: Jika kopling mati, hentikan seluruh operasi sensor.
        if (!koplingAktif) return 

        if (event == null) return
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

    private fun pemindaiMendalam(node: AccessibilityNodeInfo?, targetMatriks: Array<String>, eksekusiKlik: Boolean): Boolean {
        if (node == null) return false
        val teksNode = node.text?.toString()?.lowercase() ?: ""
        val deskripsiNode = node.contentDescription?.toString()?.lowercase() ?: ""

        var cocok = false
        for (kataKunci in targetMatriks) {
            if (teksNode.contains(kataKunci) || deskripsiNode.contains(kataKunci)) {
                cocok = true
                break
            }
        }

        if (cocok) {
            if (node.isClickable) {
                if (eksekusiKlik) node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            } else if (node.parent != null && node.parent.isClickable) {
                if (eksekusiKlik) node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }

        for (i in 0 until node.childCount) {
            if (pemindaiMendalam(node.getChild(i), targetMatriks, eksekusiKlik)) {
                return true
            }
        }
        return false
    }

    private fun tarikMundurLengan() {
        Thread.sleep(800)
        performGlobalAction(GLOBAL_ACTION_BACK)
        Thread.sleep(400)
        performGlobalAction(GLOBAL_ACTION_BACK)
        statusOperasi = STATUS_MENCARI_PENYIMPANAN
    }

    override fun onInterrupt() {
        statusOperasi = STATUS_MENCARI_PENYIMPANAN
    }
}
