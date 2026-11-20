plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.intruphon.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.intruphon.app"
        minSdk = 29
        targetSdk = 35
        // The CI run number, so every inspection build counts as an update and says which build it is.
        val buildNumber = System.getenv("GITHUB_RUN_NUMBER")
        versionCode = buildNumber?.toIntOrNull() ?: 1
        // The only place the version lives is gradle.properties (APP_VERSION); the CI reads it from there too.
        versionName = providers.gradleProperty("APP_VERSION").get()
        buildConfigField("String", "BUILD", "\"${buildNumber ?: "local"}\"")
    }

    signingConfigs {
        create("release") {
            System.getenv("ANDROID_KEYSTORE_PATH")?.let {
                storeFile = file(it)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true      // R8: removes unused code and optimizes
            isShrinkResources = true    // removes unused resources
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("ANDROID_KEYSTORE_PATH") != null)
                signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true   // required by NewPipeExtractor below Android 13
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)

    // Link reading for supported sites (GPL-3.0, same license as this project)
    implementation(libs.newpipe.extractor)
    // The extractor needs an HTTP client supplied by the app
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)
}
