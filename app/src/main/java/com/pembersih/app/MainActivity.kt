    private fun eksekusiPembersihanTersembunyiOtomatis() {
        if (!cekIzinRobotik()) {
            Toast.makeText(this, "Akses Lengan Robotik belum diaktifkan.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            return
        }

        // ALIRKAN LISTRIK: Aktifkan kopling mesin
        LenganRobotikService.koplingAktif = true
        Toast.makeText(this, "Mesin beroperasi. JANGAN sentuh layar.", Toast.LENGTH_LONG).show()
        
        val handler = Handler(Looper.getMainLooper())
        var jedaWaktu = 0L
        val intervalMesin = 3500L // Berikan ruang 3.5 detik per aplikasi

        // Lempar aplikasi ke layar satu per satu
        for (paketApp in packageList) {
            handler.postDelayed({
                bukaPengaturanAplikasi(paketApp)
            }, jedaWaktu)
            
            jedaWaktu += intervalMesin 
        }

        // PROTOKOL PENGHENTIAN: Dieksekusi setelah seluruh aplikasi dilempar
        handler.postDelayed({
            // PUTUS ARUS: Matikan sensor robotik
            LenganRobotikService.koplingAktif = false 
            
            // Tarik paksa layar Panel Kendali kembali ke depan
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            
            Toast.makeText(this, "Siklus Pabrik Selesai. Lengan Robotik Dimatikan.", Toast.LENGTH_LONG).show()
        }, jedaWaktu)
    }
