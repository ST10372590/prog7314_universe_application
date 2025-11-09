pluginManagement {
    repositories {
        google() // Required for Google plugins like 'com.google.gms.google-services'
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Universe"
include(":app")
