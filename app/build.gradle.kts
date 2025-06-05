// app/build.gradle.kts

plugins {
    // Alias do wtyczki Android Application (com.android.application)
    alias(libs.plugins.android.application)

    // Alias do wtyczki Kotlin Android (org.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.android)

    // Alias do wtyczki Kotlin Compose (jeśli korzystasz z Compose UI)
    alias(libs.plugins.kotlin.compose)

    // Wtyczka Google Services – używana do konfiguracji Firebase w module app
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.projektweterynarzapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.projektweterynarzapp"
        minSdk = 26
        targetSdk = 35
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core Android i Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Testy
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // --------------------------------------------
    // Firebase – BOM + konkretne biblioteki
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))

    // Jeśli chcesz później skorzystać z Analytics (opcjonalnie)
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Uwierzytelnianie (Auth)
    implementation("com.google.firebase:firebase-auth-ktx")

    // Firestore (baza danych dokumentowa)
    implementation("com.google.firebase:firebase-firestore-ktx")

    implementation(libs.androidx.navigation.compose)
    implementation("androidx.compose.material:material-icons-extended")


}
