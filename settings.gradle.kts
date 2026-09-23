pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Fallback: Maven Central ha rimosso il group com.arthenica (ffmpeg-kit),
        // ma il mirror ufficiale Google lo serve ancora.
        maven {
            url = uri("https://maven-central.storage-download.googleapis.com/maven2")
            content { includeGroup("com.arthenica") }
        }
    }
}

rootProject.name = "ytmp3"
include(":app")
