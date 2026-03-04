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
include(":feature:analyzer")
include(":feature:archive")
include(":feature:browser")
include(":feature:dashboard")
include(":feature:duplicates")
include(":feature:editor")
include(":feature:organizer")
include(":feature:player")
include(":feature:server")
include(":feature:settings")
include(":feature:trash")
include(":feature:viewer")
