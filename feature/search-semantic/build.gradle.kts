plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.objectbox)
}

android {
    namespace = "filey.app.feature.search.semantic"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Compose — SemanticSearchScreen için gerekli
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.androidx.lifecycle.viewmodel)

    // TensorFlow Lite
    implementation(libs.tensorflowLite)
    implementation(libs.tensorflowLiteGpu)
    implementation(libs.tensorflowLiteSupport)

    // ObjectBox
    implementation(libs.objectbox.android)
    implementation(libs.objectbox.kotlin)

    // WorkManager
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
}
