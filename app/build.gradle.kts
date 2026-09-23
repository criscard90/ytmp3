import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Versione: locale 1.0.0, in CI 1.0.<run_number> (ad ogni push sale il build number)
val ciRunNumber = (findProperty("ciRunNumber") as String?)?.toIntOrNull() ?: 0

// Keystore di release opzionale: se keystore.properties non esiste si firma con la debug key
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}
val hasReleaseKeystore = keystoreProps.getProperty("storeFile") != null

android {
    namespace = "com.criscard90.ytmp3"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.criscard90.ytmp3"
        minSdk = 29
        targetSdk = 34
        versionCode = 1 + ciRunNumber
        versionName = "1.0.$ciRunNumber"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Nessun minify: garantisce compatibilità di ffmpeg-kit/JNI e Compose.
            // Gli split per ABI tengono bassa la dimensione degli APK.
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Un APK per architettura + uno universale, per release più leggere
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Comprime le .so dentro l'APK: APK più leggero da scaricare
            // (le librerie vengono estratte all'installazione)
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // AndroidX / Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Thumbnail dei risultati di ricerca
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Riproduzione video in-app (Media3/ExoPlayer)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // HTTP (ricerca YouTube + download dello stream audio)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // FFmpeg con libmp3lame: conversione MP3 320 kbps
    implementation("com.arthenica:ffmpeg-kit-audio:6.0.LTS")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
