cd /sdcard/Filey
cat > settings.gradle.kts << 'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Filey"

include(":app")
include(":core")
include(":feature:browser")
include(":feature:viewer")
include(":feature:player")
include(":feature:editor")
include(":feature:archive")
EOF