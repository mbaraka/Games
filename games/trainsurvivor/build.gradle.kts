plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Game version (shown in Settings, and the co-op protocol number): the number of commits that have
// touched this game's code, so it goes up by one with every update to it and nothing else. Includes
// both this module's current path and the paths this code (and the shared coop package) lived at
// before the move to a module per game, so the count carries on from where it was - see AGENTS.md's
// "Splitting a game into its own module" for the recipe.
fun git(vararg args: String): String? = try {
    val process = ProcessBuilder(listOf("git") + args).directory(rootDir).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText().trim()
    if (process.waitFor() == 0) output else null
} catch (e: Exception) {
    null
}

fun gameBuild(vararg paths: String): Int =
    git(*(arrayOf("rev-list", "--count", "HEAD", "--") + paths))?.toIntOrNull() ?: 0

val trainSurvivorBuild = gameBuild(
    "games/trainsurvivor/src/main/java/com/baraka/game/trainsurvivor",
    "app/src/main/java/com/baraka/game/trainsurvivor",
    "core/src/main/java/com/baraka/game/coop",
    "app/src/main/java/com/baraka/game/coop"
)

android {
    namespace = "com.baraka.game.trainsurvivor"
    compileSdk = 36

    defaultConfig {
        minSdk = 27
        buildConfigField("int", "TRAIN_SURVIVOR_BUILD", "$trainSurvivorBuild")
        buildConfigField("String", "TRAIN_SURVIVOR_VERSION", "\"1.$trainSurvivorBuild\"")
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

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation(libs.androidx.ui.tooling)
}
