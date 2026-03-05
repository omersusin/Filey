pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.objectbox.io") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.objectbox.io") }
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
include(":feature:search-semantic")
include(":feature:smart-tags")
include(":feature:vault")
include(":feature:viewer")
