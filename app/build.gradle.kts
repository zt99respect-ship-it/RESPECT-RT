import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

fun readFirebaseField(rootDir: File, key: String, fallback: String = ""): String {
    val configFile = File(rootDir, "firebase-applet-config.json")
    if (!configFile.exists()) return fallback
    val content = configFile.readText()
    val match = Regex(""""$key"\s*:\s*"([^"]*)"""").find(content)
    return match?.groupValues?.getOrNull(1) ?: fallback
}

android {
    namespace = "com.respect.community"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.respect.community"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "KICK_CLIENT_ID", "\"01M73WDEYD7WMR9TG5X63CTQ54\"")
        buildConfigField("String", "KICK_CLIENT_SECRET", "\"755f654101e5fe8ec563cd844931618e095924bdbb631b263191c9abb63779c4\"")
        buildConfigField("String", "KICK_REDIRECT_URI", "\"respect://oauth/callback\"")
        buildConfigField("String", "ADMIN_EMAIL", "\"zt.99.respect@gmail.com\"")
        buildConfigField("String", "ADMIN_PASSWORD", "\"Slayer_rt\"")

        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${readFirebaseField(rootDir, "projectId")}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${readFirebaseField(rootDir, "appId")}\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${readFirebaseField(rootDir, "apiKey")}\"")
        buildConfigField("String", "FIREBASE_AUTH_DOMAIN", "\"${readFirebaseField(rootDir, "authDomain")}\"")
        buildConfigField("String", "FIREBASE_DATABASE_ID", "\"${readFirebaseField(rootDir, "firestoreDatabaseId")}\"")
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"${readFirebaseField(rootDir, "storageBucket")}\"")
        buildConfigField("String", "FIREBASE_SENDER_ID", "\"${readFirebaseField(rootDir, "messagingSenderId")}\"")
    }

    buildTypes {
        debug {
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.fragment:fragment-ktx:1.8.4")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // Haze Glassmorphism Blur
    implementation("dev.chrisbanes.haze:haze:1.1.1")

    // Firebase (Auth, Firestore, FCM, Remote Config)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-config-ktx")

    // WorkManager for 10-min KICK Analytics & Custom Push
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Compressor for safe <5MB image compression before Telegraph upload
    implementation("id.zelory:compressor:3.0.1")

    // Networking: Retrofit + OkHttp + Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Media3 ExoPlayer (HLS Streaming, Clips & Quality TrackSelection)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Chrome Custom Tabs for KICK OAuth2
    implementation("androidx.browser:browser:1.8.0")

    // Coil Image Loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Coroutines & Play Services Await
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
