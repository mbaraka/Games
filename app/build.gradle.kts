import java.util.Properties
import org.gradle.api.tasks.Copy

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries)
}

// Release signing, read from the gitignored local.properties (same file Android Studio
// already keeps sdk.dir in) so a local `bundleRelease`/`assembleRelease` can be signed
// without exporting shell env vars first. CI (when re-enabled) still works via the
// RELEASE_KEYSTORE_* env vars as a fallback - see RELEASING.md.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

// Versioning policy (Play Store track): versionName is MAJOR.MINOR.PATCH - MAJOR bumps when
// the app is substantially overhauled, MINOR when a new game is added, PATCH for small
// fixes/tweaks. Override from CI with -PappMajor=N -PappMinor=N -PappPatch=N if needed.
val appMajor = (project.findProperty("appMajor") as String?)?.toInt() ?: 0
val appMinor = (project.findProperty("appMinor") as String?)?.toInt() ?: 1
val appPatch = (project.findProperty("appPatch") as String?)?.toInt() ?: 0
val appVersionName = "$appMajor.$appMinor.$appPatch"

// versionCode is a separate, hand-bumped counter (1, 2, 3, ...) - Play just needs each upload's
// code to be strictly higher than the last one, so there's no need to derive it from
// versionName. Bump this by one every time you cut a new Play release.
val appVersionCode = 3

// Per-game versions. The app holds several games, so a game's version is NOT the app version: it is the number of commits that
// touched that game's code, so it goes up by one with every update to that game (and only that game). Games with co-op also
// use it to tell two phones on different builds apart. Add a line here for a new game and expose it through BuildConfig below.
fun git(vararg args: String): String? = try {
    val process = ProcessBuilder(listOf("git") + args).directory(rootDir).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText().trim()
    if (process.waitFor() == 0) output else null
} catch (e: Exception) {
    null
}

fun gameBuild(vararg paths: String): Int =
    git(*(arrayOf("rev-list", "--count", "HEAD", "--") + paths))?.toIntOrNull() ?: 0

val trainSurvivorBuild = gameBuild("app/src/main/java/com/baraka/game/trainsurvivor", "app/src/main/java/com/baraka/game/coop")
val knightSurvivorBuild = gameBuild("app/src/main/java/com/baraka/game/knightsurvivor", "app/src/main/java/com/baraka/game/coop")
val minesweeperBuild = gameBuild("app/src/main/java/com/baraka/game/minesweeper", "app/src/main/java/com/baraka/game/coop")
val clashDecksBuild = gameBuild("app/src/main/java/com/baraka/game/clashdecks", "app/src/main/java/com/baraka/game/coop")
val battleshipBuild = gameBuild("app/src/main/java/com/baraka/game/battleship")
val dreadnoughtBuild = gameBuild("app/src/main/java/com/baraka/game/dreadnought")

android {
    namespace = "com.baraka.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.baraka.game"
        minSdk = 27
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("int", "TRAIN_SURVIVOR_BUILD", "$trainSurvivorBuild")
        buildConfigField("String", "TRAIN_SURVIVOR_VERSION", "\"1.$trainSurvivorBuild\"")
        buildConfigField("int", "KNIGHT_SURVIVOR_BUILD", "$knightSurvivorBuild")
        buildConfigField("String", "KNIGHT_SURVIVOR_VERSION", "\"1.$knightSurvivorBuild\"")
        buildConfigField("int", "MINESWEEPER_BUILD", "$minesweeperBuild")
        buildConfigField("String", "MINESWEEPER_VERSION", "\"1.$minesweeperBuild\"")
        buildConfigField("int", "CLASH_DECKS_BUILD", "$clashDecksBuild")
        buildConfigField("String", "CLASH_DECKS_VERSION", "\"1.$clashDecksBuild\"")
        buildConfigField("String", "BATTLESHIP_VERSION", "\"1.$battleshipBuild\"")
        buildConfigField("String", "DREADNOUGHT_VERSION", "\"1.$dreadnoughtBuild\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Only registered when a release keystore is supplied (local.properties for a
        // local build, or the RELEASE_KEYSTORE_* env vars for CI), so a plain debug
        // build still works with zero setup. See RELEASING.md.
        val keystorePath = localProperties.getProperty("RELEASE_KEYSTORE_FILE")
            ?: System.getenv("RELEASE_KEYSTORE_PATH")
        if (!keystorePath.isNullOrBlank()) {
            create("release") {
                // rootProject.file(), not file(): local.properties lives at the project
                // root, so a relative RELEASE_KEYSTORE_FILE resolves from there, not from
                // this app module's own directory.
                storeFile = rootProject.file(keystorePath)
                storePassword = localProperties.getProperty("RELEASE_KEYSTORE_PASSWORD")
                    ?: System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                    ?: System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
                    ?: System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
            output.outputFileName = "baraka-${variant.versionName}-${variant.versionCode}-${variant.buildType.name}.apk"
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
        buildConfig = true
    }
}

// AGP's own bundle task always names its output "app-<buildType>.aab" with no hook to rename it
// directly (unlike the APK output above) - so this adds a versioned copy alongside it into
// app/release/, the same folder Android Studio's "Generate Signed App Bundle" wizard writes to,
// so every build (IDE or CLI) lands in one place, named so multiple uploads are easy to tell
// apart. Also a natural spot to drop store-listing assets (screenshots, feature graphic) later.
afterEvaluate {
    android.buildTypes.names.forEach { buildTypeName ->
        val capitalized = buildTypeName.replaceFirstChar { it.uppercase() }
        val bundleTaskName = "bundle$capitalized"
        if (tasks.findByName(bundleTaskName) != null) {
            tasks.register<Copy>("renameBundle$capitalized") {
                // A separate directory, not AGP's own outputs/bundle/<type> - writing back into
                // that same directory trips Gradle's task-output-overlap validation against
                // AGP's own produce<Type>BundleIdeListingFile task, which also writes there.
                from(layout.buildDirectory.dir("outputs/bundle/$buildTypeName"))
                include("app-$buildTypeName.aab")
                rename {
                    "baraka-${android.defaultConfig.versionName}-${android.defaultConfig.versionCode}-$buildTypeName.aab"
                }
                into(rootProject.file("app/release"))
            }
            tasks.named(bundleTaskName) {
                finalizedBy("renameBundle$capitalized")
            }
        }
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
    implementation(libs.aboutlibraries.compose.m3)

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
