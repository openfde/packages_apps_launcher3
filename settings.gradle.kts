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
    }
}

rootProject.name = "Launcher3QuickStep"

include(":app")
include(":widgetpicker")

project(":app").projectDir = file("gradle-modules/app")
project(":widgetpicker").projectDir = file("gradle-modules/widgetpicker")
