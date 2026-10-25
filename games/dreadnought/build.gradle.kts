plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Game version (shown in Settings): the number of commits that have touched this game's
// code, so it goes up by one with every update to it and nothing else. Includes both this
// module's current path and the path this code lived at before the move to a module per
// game, so the count carries on from where it was - see AGENTS.md's "Splitting a game into
// its own module" for the recipe.
fun git(vararg args: String): String? = try {
    val process = ProcessBuilder(listOf("git") + args).directory(rootDir).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText().trim()
    if (process.waitFor() == 0) output else null
} catch (e: Exception) {
    null
}

fun gameBuild(vararg paths: String): Int =
    git(*(arrayOf("rev-list", "--count", "HEAD", "--") + paths))?.toIntOrNull() ?: 0

val dreadnoughtBuild = gameBuild(
    "games/dreadnought/src/main/java/com/baraka/game/dreadnought",
    "app/src/main/java/com/baraka/game/dreadnought"
)

android {
    namespace = "com.baraka.game.dreadnought"
    compileSdk = 36

    defaultConfig {
        minSdk = 27
        buildConfigField("int", "DREADNOUGHT_BUILD", "$dreadnoughtBuild")
        buildConfigField("String", "DREADNOUGHT_VERSION", "\"1.$dreadnoughtBuild\"")
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
