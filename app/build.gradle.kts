plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Versioning policy: MAJOR stays 1, MINOR bumps when a new game is added,
// PATCH bumps for small fixes/tweaks. Override from CI with -PappMinor=N -PappPatch=N
// (e.g. driven by a git tag like v1.4.2) so a tag push controls the shipped version.
val appMajor = (project.findProperty("appMajor") as String?)?.toInt() ?: 1
val appMinor = (project.findProperty("appMinor") as String?)?.toInt() ?: 0
val appPatch = (project.findProperty("appPatch") as String?)?.toInt() ?: 0
val appVersionName = "$appMajor.$appMinor.$appPatch"
val appVersionCode = appMajor * 10_000 + appMinor * 100 + appPatch

android {
    namespace = "com.baraka.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.baraka.game"
        minSdk = 27
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Only registered when a release keystore is supplied (e.g. via CI secrets),
        // so local debug builds work with zero setup. See RELEASING.md.
        val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
        if (!keystorePath.isNullOrBlank()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "baraka-${variant.versionName}-${variant.buildType.name}.apk"
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
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // 3D Game Engine - LibGDX (Core dependencies only)
    implementation("com.badlogicgames.gdx:gdx:1.12.0")
    implementation("com.badlogicgames.gdx:gdx-backend-android:1.12.0")
    
    // Additional 3D support
    implementation("com.google.android.material:material:1.11.0")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}