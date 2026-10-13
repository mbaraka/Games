plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Versioning policy: MAJOR stays 1, MINOR bumps when a new game is added, PATCH bumps for small fixes/tweaks.
// Releases: CI passes -PappMajor/-PappMinor/-PappPatch from the git tag (v1.4.2), so the tag IS the version.
// Local builds: the version is the latest tag plus the number of commits since it ("1.2.0+17"), so it goes up by one with
// every commit and every installed build says exactly which update it is. versionCode follows (tag code + commits since).
fun git(vararg args: String): String? = try {
    val process = ProcessBuilder(listOf("git") + args).directory(rootDir).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText().trim()
    if (process.waitFor() == 0) output else null
} catch (e: Exception) {
    null
}

val releaseBuild = project.hasProperty("appMajor") || project.hasProperty("appMinor") || project.hasProperty("appPatch")
val described = Regex("""v(\d+)\.(\d+)\.(\d+)-(\d+)-g[0-9a-f]+""").find(git("describe", "--tags", "--long", "--match", "v[0-9]*") ?: "")
val tagParts = described?.groupValues?.drop(1)?.map { it.toInt() }
val commitsSinceTag = if (releaseBuild) 0 else (tagParts?.get(3) ?: 0)
val appMajor = (project.findProperty("appMajor") as String?)?.toInt() ?: tagParts?.get(0) ?: 1
val appMinor = (project.findProperty("appMinor") as String?)?.toInt() ?: tagParts?.get(1) ?: 0
val appPatch = (project.findProperty("appPatch") as String?)?.toInt() ?: tagParts?.get(2) ?: 0
val appVersionName = "$appMajor.$appMinor.$appPatch" + if (commitsSinceTag > 0) "+$commitsSinceTag" else ""
val appVersionCode = appMajor * 10_000 + appMinor * 100 + appPatch + commitsSinceTag

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