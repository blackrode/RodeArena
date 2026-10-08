plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
 namespace = "com.rodearena.app"; compileSdk = 35
    defaultConfig {
        applicationId = "com.rodearena.app"; minSdk = 26; targetSdk = 35
        versionCode = 1; versionName = "1.0"
        // Replace with the deployed HTTPS RodeArena API URL for production builds.
        buildConfigField("String", "API_BASE_URL", "\"https://YOUR-RODEARENA-API.example/\"")
    }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
