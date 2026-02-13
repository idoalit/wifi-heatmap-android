## Plan: Splash, Login Google, dan Firebase Storage

Tambahkan dependensi dan konfigurasi Firebase + Google Sign-In, buat splash screen berbasis tema, lalu bangun alur autentikasi untuk memaksa login sebelum akses fitur. Integrasikan penyimpanan data ke Firebase (mis. Firestore) lewat repository yang dapat sinkron dengan Room. Perubahan difokuskan pada Gradle, manifest/theme, nav flow Compose, serta modul Koin dan layer data.

### Steps 4
1. Perbarui dependensi di [gradle/libs.versions.toml](gradle/libs.versions.toml) dan [app/build.gradle.kts](app/build.gradle.kts) untuk `core-splashscreen`, Firebase BOM, `firebase-auth`, `play-services-auth`, `firebase-firestore-ktx`, serta plugin `google-services`.
2. Tambahkan konfigurasi Firebase dengan `google-services.json` dan set tema splash di [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml) dan [app/src/main/res/values/themes.xml](app/src/main/res/values/themes.xml) (logo/warna).
3. Buat alur autentikasi Compose: layar splash/auth, status login di ViewModel, dan gating nav di [app/src/main/java/id/klaras/wifilogger/MainActivity.kt](app/src/main/java/id/klaras/wifilogger/MainActivity.kt).
4. Integrasikan Firebase storage ke repository dan Koin: update [app/src/main/java/id/klaras/wifilogger/di/AppModules.kt](app/src/main/java/id/klaras/wifilogger/di/AppModules.kt) dan repository data agar sinkron dengan Room.

### Further Considerations 2
1. Penyimpanan Firebase: Firestore (dokumen) atau Realtime Database (streaming)? Pilih A/B sesuai kebutuhan query.
2. Sinkronisasi data: offline-first (Room utama) atau cloud-first (Firebase utama)? Pilih strategi yang diinginkan.

