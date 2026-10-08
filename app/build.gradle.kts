plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") version "4.4.4" apply false
}

android {
    namespace = "com.NicTvChanel_Plus"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.NicTvChanel_Plus"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
        implementation("com.google.firebase:firebase-analytics")
        implementation("com.google.firebase:firebase-config")

        // Media3 ExoPlayer para streaming m3u8
        implementation("androidx.media3:media3-exoplayer:1.2.0")
        implementation("androidx.media3:media3-ui:1.2.0")
        implementation("androidx.media3:media3-exoplayer-hls:1.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation("com.google.android.material:material:1.11.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")

}