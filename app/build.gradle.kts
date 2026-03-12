plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "filey.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "filey.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
    implementation(project(":feature:analyzer"))
    implementation(project(":feature:archive"))
    implementation(project(":feature:browser"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:duplicates"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:organizer"))
    implementation(project(":feature:player"))
    implementation(project(":feature:server"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:trash"))
    implementation(project(":feature:security"))
    implementation(project(":feature:vault"))
    implementation(project(":feature:viewer"))
    implementation(project(":feature:search-semantic"))
    implementation(project(":feature:smart-tags"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.coil.compose)
    implementation(libs.libsu.core)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.compose.ui.tooling)
}
