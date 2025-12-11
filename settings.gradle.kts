
rootProject.name = "Toolshot"

pluginManagement {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs")
    }
}
